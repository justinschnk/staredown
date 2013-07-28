package com.opentok.android.demo.helloworld;

import android.app.Activity;
import android.content.Context;
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
import com.opentok.android.OpentokException;
import com.opentok.android.Publisher;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.demo.EpicPublisher;
import com.opentok.android.demo.EpicSubscriber;
import com.opentok.android.demo.EpicVideoCaptureManager;
import com.opentok.impl.SubscriberImpl;
import com.opentok.media.AbstractCodec;
import com.opentok.media.AndroidSurfaceRenderer;
import com.opentok.media.avc.AvcDecoder;
import com.opentok.rtp.codec.AvcDepacketizer;
import com.opentok.android.demo.R;
import com.opentok.stat.SubscriberStatistics;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This application demonstrates the basic workflow for getting started with the OpenTok Android SDK.
 * Basic hello-world activity shows publishing audio and video and subscribing to an audio and video stream
 */
public class HelloWorldActivity extends Activity implements Publisher.Listener, Subscriber.Listener, Session.Listener {

    private static final String LOGTAG = "demo-hello-world";
    // automatically connect during Activity.onCreate
    private static final boolean AUTO_CONNECT = true;
    // automatically publish during Session.Listener.onSessionConnected
    private static final boolean AUTO_PUBLISH = true;
    // automatically subscribe during Session.Listener.onSessionReceivedStream IFF stream is our own
    private static final boolean SUBSCRIBE_TO_SELF = false;


    ImageView iv;

    /* Fill the following variables using your own Project info from the Dashboard */
    // Replace with your generated Session ID
    private static final String SESSION_ID = "1_MX4zNjUwMjIxMn4xMjcuMC4wLjF-U2F0IEp1bCAyNyAxMzo0OTo0OSBQRFQgMjAxM34wLjI5NjM1OTN-";
    // Replace with your generated Token (use Project Tools or from a server-side library)
    private static final String TOKEN = "T1==cGFydG5lcl9pZD0zNjUwMjIxMiZzZGtfdmVyc2lvbj10YnJ1YnktdGJyYi12MC45MS4yMDExLTAyLTE3JnNpZz0xZDM2M2M5NTcwNDNlMmE0ZWJlZjQ5M2RhN2ViYzAyZTYyNzM0MzBmOnJvbGU9cHVibGlzaGVyJnNlc3Npb25faWQ9MV9NWDR6TmpVd01qSXhNbjR4TWpjdU1DNHdMakYtVTJGMElFcDFiQ0F5TnlBeE16bzBPVG8wT1NCUVJGUWdNakF4TTM0d0xqSTVOak0xT1ROLSZjcmVhdGVfdGltZT0xMzc0OTU4MjE4Jm5vbmNlPTAuNDIzNjEzMjE1MTA1MDcxOSZleHBpcmVfdGltZT0xMzc1MDQ0NjE4JmNvbm5lY3Rpb25fZGF0YT0=";
    private RelativeLayout publisherViewContainer;
    private RelativeLayout subscriberViewContainer;
    private Publisher publisher;
    private Subscriber subscriber;
    private Session session;
    private WakeLock wakeLock;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
                    if (toast != null) {
                        toast.cancel();
                    }
                    boolean blinked = !eyesFound(b);
                    if (view && blinked) {
                        toast = Toast.makeText(getApplicationContext(), blinked + "", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    b = null;
                }
            };


        }

    }

    private Toast toast;

    private int RGB_MASK = 0x00ffffff;

    public boolean eyesFound(Bitmap b) {
        int width = b.getWidth();
        int height = b.getHeight();

        Log.d(LOGTAG, "finding eyes... "+b.getConfig()+", "+width+"x"+height);

        FaceDetector faceDetector = new FaceDetector(width, height, 1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        faceDetector.findFaces(b, faces);
        FaceDetector.Face f = faces[0];

        if(f == null) {
            Log.d(LOGTAG, "no face found");
            return false;
        }

        PointF midPoint = new PointF();
        f.getMidPoint(midPoint);
        Float dist = f.eyesDistance();

        int leftX = (int) (midPoint.x - dist/2);
        int leftY = (int) midPoint.y;

        int rightX = (int) (midPoint.x + dist/2);
        int rightY = (int) midPoint.y;

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

            int sumColors = Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i]);

            int threshold = 255;
            if (avg < 200) {
                threshold = 0;
            } else {
                leftEyeColCount++;
            }

            pixels[i] = Color.argb(255, threshold, threshold, threshold);
        }
        //b.setPixels(pixels, 0, eyeWidth, startEyeX, startEyeY, eyeWidth, eyeHeight);

        // right eye
        startEyeX = rightX - eyeWidth/2;
        startEyeY = rightY - eyeHeight/2;

        b.getPixels(pixels, 0, eyeWidth, startEyeX, startEyeY, eyeWidth, eyeHeight);

        int rightEyeColCount = 0;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] ^= RGB_MASK;

            int pix = pixels[i];
            int avg = (int) (0.2125* Color.red(pix) + 0.7154*Color.green(pix) + 0.0721*Color.blue(pix));
            pixels[i] = Color.argb(Color.alpha(pix), avg, avg, avg);

            int sumColors = Color.red(pixels[i]) + Color.green(pixels[i]) + Color.blue(pixels[i]);

            int threshold = 255;
            if (avg < 200) {
                threshold = 0;
            } else {
                rightEyeColCount++;
            }
            pixels[i] = Color.argb(255, threshold, threshold, threshold);

        }
        //b.setPixels(pixels, 0, eyeWidth, startEyeX, startEyeY, eyeWidth, eyeHeight);
        // --

        double leftEyeRatio = leftEyeColCount/(eyeWidth*eyeHeight+0.0);
        double rightEyeRatio = rightEyeColCount/(eyeWidth*eyeHeight+0.0);

        if(avgLeftQueue.size() == 0) {
            for(int i=0; i<25; i++) avgLeftQueue.add(leftEyeRatio);
        }

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



//        Log.d(LOGTAG, "leftEyeColCount ratio: "+leftEyeColCount/(eyeWidth*eyeHeight+0.0)+", rightEyeColCount ratio: "+rightEyeColCount/(eyeWidth*eyeHeight+0.0));
//        if (leftEyeColCount > 0.09) {
//            // left eye is open
//            return true;
//        }
//        if (rightEyeRatio > 0.09) {
//            // right eye is open
//            return true;
//        }

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
}