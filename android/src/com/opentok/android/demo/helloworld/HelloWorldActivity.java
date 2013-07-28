package com.opentok.android.demo.helloworld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import android.widget.TextView;
import android.widget.Toast;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.opentok.android.OpentokException;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.demo.*;
import com.opentok.impl.SubscriberImpl;
import com.opentok.media.AbstractCodec;
import com.opentok.media.AndroidSurfaceRenderer;
import com.opentok.media.avc.AvcDecoder;
import com.opentok.rtp.codec.AvcDepacketizer;
import com.opentok.stat.SubscriberStatistics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import android.provider.Settings.Secure;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This application demonstrates the basic workflow for getting started with the OpenTok Android SDK.
 * Basic hello-world activity shows publishing audio and video and subscribing to an audio and video stream
 */
public class HelloWorldActivity extends Activity implements Publisher.Listener, Subscriber.Listener, Session.Listener, NetApiCallback{

    private static final String LOGTAG = "demo-hello-world";
    // automatically connect during Activity.onCreate
    private static final boolean AUTO_CONNECT = false;
    // automatically publish during Session.Listener.onSessionConnected
    private static final boolean AUTO_PUBLISH = true;
    // automatically subscribe during Session.Listener.onSessionReceivedStream IFF stream is our own
    private static final boolean SUBSCRIBE_TO_SELF = false;

    private static final String TAG = "HelloWorldActivity";

    private int userMe = 0;

    ImageView iv;

    /* Fill the following variables using your own Project info from the Dashboard */
    // Replace with your generated Session ID
    private static String SESSION_ID = "1_MX4zNjUwMjIxMn4xMjcuMC4wLjF-U2F0IEp1bCAyNyAxMzo0OTo0OSBQRFQgMjAxM34wLjI5NjM1OTN-";
    // Replace with your generated Token (use Project Tools or from a server-side library)
    private static String TOKEN = "T1==cGFydG5lcl9pZD0zNjUwMjIxMiZzZGtfdmVyc2lvbj10YnJ1YnktdGJyYi12MC45MS4yMDExLTAyLTE3JnNpZz0xZDM2M2M5NTcwNDNlMmE0ZWJlZjQ5M2RhN2ViYzAyZTYyNzM0MzBmOnJvbGU9cHVibGlzaGVyJnNlc3Npb25faWQ9MV9NWDR6TmpVd01qSXhNbjR4TWpjdU1DNHdMakYtVTJGMElFcDFiQ0F5TnlBeE16bzBPVG8wT1NCUVJGUWdNakF4TTM0d0xqSTVOak0xT1ROLSZjcmVhdGVfdGltZT0xMzc0OTU4MjE4Jm5vbmNlPTAuNDIzNjEzMjE1MTA1MDcxOSZleHBpcmVfdGltZT0xMzc1MDQ0NjE4JmNvbm5lY3Rpb25fZGF0YT0=";
    private RelativeLayout publisherViewContainer;
    private RelativeLayout subscriberViewContainer;
    private Publisher publisher;
    private Subscriber subscriber;
    private Session session;
    private WakeLock wakeLock;
    private TextView tv;


    NetApi netApi;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        if (userId == null) userId = "8";

        netApi = new NetApi(this, "http://ec2-54-227-163-21.compute-1.amazonaws.com:3000");
        netApi.getQueue(userId, "yooo");
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main_layout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        iv = (ImageView) findViewById(R.id.imageView);
        publisherViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        subscriberViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        tv = (TextView) findViewById(R.id.text);
        Typeface tf = Typeface.createFromAsset(this.getAssets(),
                "font.ttf");
        tv.setTypeface(tf);
        // Disable screen dimming
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

        if (AUTO_CONNECT) {
            sessionConnect();
        }
    }


    @Override
    public void onStop() {
        super.onStop();

        if (session != null) {
            session.disconnect();
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void sessionConnect() {
        session = Session.newInstance(HelloWorldActivity.this, SESSION_ID, HelloWorldActivity.this);
        session.connect(TOKEN);
    }

    @Override
    public void onSessionConnected() {

        Log.i(LOGTAG, "session connected");

        // Session is ready to publish.
        if (AUTO_PUBLISH) {
            //Create Publisher instance.
            publisher = EpicPublisher.newInstance(HelloWorldActivity.this);
            publisher.setName("My First Publisher");
            publisher.setListener(HelloWorldActivity.this);

            RelativeLayout.LayoutParams publisherViewParams =
                    new RelativeLayout.LayoutParams(publisher.getView().getLayoutParams().width,
                            publisher.getView().getLayoutParams().height);
            publisherViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            publisherViewParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            publisherViewParams.bottomMargin = dpToPx(8);
            publisherViewParams.rightMargin = dpToPx(8);
            publisherViewContainer.setLayoutParams(publisherViewParams);
            publisherViewContainer.addView(publisher.getView());
            session.publish(publisher);

            EpicVideoCaptureManager.drawcallback = new EpicVideoCaptureManager.DrawCallback() {
                @Override
                public void callback(Bitmap b) {
                    //To change body of implemented methods use File | Settings | File Templates.

                    boolean blinked = !eyesFound(b);
                    if (view && blinked && !blinkedcalled) {

                        blinkedcalled = true;
                        String splits[] = mGamesUrl.split("/");
                        String gamesHash = splits[splits.length-1];

                        Log.d(TAG, "blinked: "+gamesHash+", "+userId);
                        netApi.blink(gamesHash, userId);
                    }

                    b = null;
                }
            };


        }

    }

    boolean blinkedcalled = false;

    private int RGB_MASK = 0x00ffffff;

    Toast toast;

    public boolean eyesFound(Bitmap b2) {

        Bitmap b = b2.copy(Bitmap.Config.RGB_565, true);

        int width = b.getWidth();
        int height = b.getHeight();

        // Log.d(LOGTAG, "finding eyes... "+b.getConfig()+", "+width+"x"+height);


        FaceDetector faceDetector = new FaceDetector(width, height, 1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        faceDetector.findFaces(b, faces);
        FaceDetector.Face f = faces[0];

        if(f == null) {
            Log.d(LOGTAG, "no face found");

            if(toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(getApplicationContext(), "No face found", Toast.LENGTH_SHORT);

            return true;
        }

        PointF midPoint = new PointF();
        f.getMidPoint(midPoint);
        Float dist = f.eyesDistance();

        int leftX = (int) (midPoint.x - dist/2);
        int leftY = (int) midPoint.y;

//        int rightX = (int) (midPoint.x + dist/2);
//        int rightY = (int) midPoint.y;

        int eyeWidth = (int) (dist/2);
        int eyeHeight = (int) (dist/3);
        int startEyeX = leftX - eyeWidth/2;
        int startEyeY = leftY - eyeHeight/2;

        int [] pixels = new int[eyeWidth*eyeHeight];
        b.getPixels(pixels, 0, eyeWidth, startEyeX, startEyeY, eyeWidth, eyeHeight);

        int leftEyeColCount = 0;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] ^= RGB_MASK;

            int pix = pixels[i];
            int avg = (int) (0.2125* Color.red(pix) + 0.7154*Color.green(pix) + 0.0721*Color.blue(pix));
            pixels[i] = Color.argb(Color.alpha(pix), avg, avg, avg);


            int threshold = 255;
            if (avg < 200) {
                threshold = 0;
            } else {
                leftEyeColCount++;
            }

            pixels[i] = Color.argb(255, threshold, threshold, threshold);
        }

        b.setPixels(pixels, 0, eyeWidth, startEyeX, startEyeY, eyeWidth, eyeHeight);

        // --

        double leftEyeRatio = leftEyeColCount/(eyeWidth*eyeHeight+0.0);

        iv.setImageBitmap(b);

        if(avgLeftQueue.size() < 25) {
            avgLeftQueue.add(leftEyeRatio);
            return true;
        } else {
            double avgLeft = 0;
            for(int i=0; i<avgLeftQueue.size(); i++) {
                avgLeft += avgLeftQueue.get(i);
            }
            avgLeft /= avgLeftQueue.size();

            Log.d(LOGTAG, "avgLeft: "+avgLeft);

            if (leftEyeRatio < avgLeft*0.6666) {
                // blinked
                Log.d(LOGTAG, "blinked, leftEyeRatio was just "+leftEyeRatio);
                return false;
            }
            else {
                avgLeftQueue.remove(0);
                avgLeftQueue.add(leftEyeRatio);
                return true;
            }
        }

    }

    LinkedList<Double> avgLeftQueue = new LinkedList<Double>();

    private void zoomText(String s, int millis)  {
      zoomText(new String[] {s}, millis, 0);
    }

    private void zoomText(final String[] s, final int millis, final int i) {
        if (i >= s.length) return;
        tv.setText(s[i]);
        AnimationSet animSet = new AnimationSet(false);
        ScaleAnimation zoom = new ScaleAnimation(0, 10, 0, 10, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this

        animSet.addAnimation(zoom);
        animSet.addAnimation(fadeOut);
        animSet.setDuration(millis);

        tv.setAnimation(animSet);
        animSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //To change body of implemented methods use File | Settings | File Templates.
                tv.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //To change body of implemented methods use File | Settings | File Templates.
                tv.setVisibility(View.INVISIBLE);
                zoomText(s, millis, i + 1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        animSet.start();
    }


    @Override
    public void onSessionDroppedStream(Stream stream) {
        Log.i(LOGTAG, String.format("stream dropped", stream.toString()));
        subscriber = null;
        subscriberViewContainer.removeAllViews();
    }

    SurfaceView surfaceView;

    @SuppressWarnings("unused")
    @Override
    public void onSessionReceivedStream(final Stream stream) {
        Log.i(LOGTAG, "session received stream");

        boolean isMyStream = session.getConnection().equals(stream.getConnection());
        //If this incoming stream is our own Publisher stream and subscriberToSelf is true let's look in the mirror.
        if ((SUBSCRIBE_TO_SELF && isMyStream) || (!SUBSCRIBE_TO_SELF && !isMyStream)) {
            subscriber = Subscriber.newInstance(HelloWorldActivity.this, stream);
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels);
            View subscriberView = subscriber.getView();
            subscriberView.setLayoutParams(params);
            subscriberViewContainer.addView(subscriber.getView());
            subscriber.setListener(HelloWorldActivity.this);
            session.subscribe(subscriber);

            zoomText(new String[] {"Ready?", "Set", "Stare!"}, 1000, 0);
        }
    }

    @Override
    public void onSubscriberConnected(Subscriber subscriber) {
        Log.i(LOGTAG, "subscriber connected");
        view = true;

    }

    boolean view = false;

    @Override
    public void onSessionDisconnected() {
        gameOn = false;
        Log.i(LOGTAG, "session disconnected");
    }

    @Override
    public void onSessionException(OpentokException exception) {
        Log.e(LOGTAG, "session failed! " + exception.toString());
    }

    @Override
    public void onSubscriberException(Subscriber subscriber, OpentokException exception) {
        Log.i(LOGTAG, "subscriber " + subscriber + " failed! " + exception.toString());
    }

    @Override
    public void onPublisherChangedCamera(int cameraId) {
        Log.i(LOGTAG, "publisher changed camera to cameraId: " + cameraId);
    }

    @Override
    public void onPublisherException(OpentokException exception) {
        Log.i(LOGTAG, "publisher failed! " + exception.toString());
    }

    @Override
    public void onPublisherStreamingStarted() {
        Log.i(LOGTAG, "publisher is streaming!");
    }

    @Override
    public void onPublisherStreamingStopped() {
        Log.i(LOGTAG, "publisher disconnected");
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public Session getSession() {
        return session;
    }

    public RelativeLayout getPublisherView() {
        return publisherViewContainer;
    }

    public RelativeLayout getSubscriberView() {
        return subscriberViewContainer;
    }


    /**
     * Converts dp to real pixels, according to the screen density.
     * @param dp A number of density-independent pixels.
     * @return The equivalent number of real pixels.
     */
    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

    String mGamesUrl;

    @Override
    public void queueCallback(QueueData queueData) {
        Log.d(TAG, "queueCallback: "+queueData.toString());

        SESSION_ID = queueData.getmSessionId();
        TOKEN = queueData.getmToken();

        Log.d(TAG, "sess: "+SESSION_ID);
        Log.d(TAG, "tok: "+TOKEN);

        if(queueData.getmStatus().equals("waiting")) {
            userMe = 1;
            Firebase firebase = new Firebase(queueData.getmFirebase());

            firebase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "value listener, data change, "+dataSnapshot.getValue()+", "+dataSnapshot.getName());

                    if(dataSnapshot.getValue() == null) {
                        return;
                    }

                    String gamesUrl = (String)((Map)dataSnapshot.getValue()).get("url");

                    if(gamesUrl.contains("/games/")) {

                        //listenToGames(gamesUrl);
                        mGamesUrl = gamesUrl;

                        if(!gameOn) {
                            listenToGames(mGamesUrl);
                            gameOn = true;
                        }
                    }

                }

                @Override
                public void onCancelled() {
                }
            });
        }
        else {
            userMe = 2;
            String gamesUrl = queueData.getmFirebase();
            mGamesUrl = gamesUrl;
            listenToGames(gamesUrl);
        }
    }


    boolean gameOn = false;

    @Override
    public void gamesCallback(String someHash) {
        Log.d(TAG, "im games callback "+someHash);
        String firebaseUrl = "https://staredown.firebaseio.com/games/" + someHash;
        Firebase firebase = new Firebase(firebaseUrl);
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "games callback: "+dataSnapshot.getValue());

                long winner = (Long)((Map)dataSnapshot.getValue()).get("winner");


                if(winner == userMe) {
                    // I win
                    Intent i = new Intent(HelloWorldActivity.this, GameOver.class);
                    i.putExtra("didIWin", true);
                    startActivity(i);
                    finish();
                }
                else if (winner != 0){
                    // I lose
                    Intent i = new Intent(HelloWorldActivity.this, GameOver.class);
                    i.putExtra("didIWin", false);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled() {
            }
        });
    }


    @Override
    public void leaderboardCallback(List<NetApi.User> users) {
    }

    @Override
    public void blink() {
        this.finish();
    }

    public void listenToGames(String gamesUrl) {
        if(gamesUrl == null) {
            Log.d(TAG, "listenToGames is null.");
            return;
        }
        Log.d(TAG, "in listen to games for "+gamesUrl);
        String splits[] = gamesUrl.split("/");
        netApi.sendGame(splits[splits.length-1]);

        sessionConnect();
    }


}
