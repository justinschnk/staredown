package com.opentok.android.demo;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import com.opentok.android.OpentokException;
import com.opentok.android.OpentokException.Domain;
import com.opentok.android.OpentokException.ErrorCode;
import com.opentok.android.Publisher;
import com.opentok.android.Stream;
import com.opentok.capture.AndroidAudioCaptureManager;
import com.opentok.capture.VideoCaptureManager;
import com.opentok.capture.VideoCaptureManager.Listener;
import com.opentok.client.DeviceInfo;
import com.opentok.client.FeatureCheck;
import com.opentok.clientevents.ClientLogger;
import com.opentok.clientevents.ClientLogger.Reportable;
import com.opentok.delegate.PublisherDelegate;
import com.opentok.impl.PublisherImpl;
import com.opentok.impl.SessionImpl;
import com.opentok.impl.StreamImpl;
import com.opentok.media.AndroidPipelineConfigurator;
import com.opentok.media.AndroidPipelineConfigurator.Config;
import com.opentok.media.avc.AndroidAvcEncoder;
import com.opentok.media.avc.AvcStreamParser;
import com.opentok.media.avc.AvcStreamParser.ParameterSetsListener;
import com.opentok.media.speex.SpeexNativeCodec;
import com.opentok.rtp.RtpConsumer;
import com.opentok.rtp.codec.AvcPacketizer;
import com.opentok.rtp.codec.SpeexPacketizer;
import com.opentok.rtsp.RtspClientDriver;
import com.opentok.rtsp.RtspClientDriver.Mode;
import com.opentok.runtime.Workers;
import com.opentok.sdp.SDPFactory;
import com.opentok.sdp.SDPParseException;
import com.opentok.sdp.SessionDescription;
import com.opentok.stat.PublisherStatistics;
import com.opentok.view.AspectRatioLayout;
import com.opentok.view.OverlayView;
import com.opentok.view.UnderlayView;
import com.opentok.webservices.SessionInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import tokbox.org.slf4j.Logger;
import tokbox.org.slf4j.LoggerFactory;

public class EpicPublisher extends PublisherImpl
        implements AvcStreamParser.ParameterSetsListener, RtspClientDriver.Listener, ClientLogger.Reportable, VideoCaptureManager.Listener, PublisherStatistics.Listener
{
    private static Logger logger = LoggerFactory.getLogger("opentok-publisher");
    private QosSender qosSender;
    private ScheduledFuture<?> qosSenderFutureFirst;
    private ScheduledFuture<?> qosSenderFuturePeriodic;
    private EpicVideoCaptureManager videoCaptureManager;
    private int cameraId = -1;
    private Camera camera;
    private AndroidAvcEncoder avcEncoder;
    private long streamingStartedTime;
    private long publishRequestedTime;
    private AvcPacketizer avcPacketizer;
    private AndroidAudioCaptureManager audioCaptureManager;
    private PublisherStatistics publisherStats;
    private Context context;
    private AspectRatioLayout publisherView;
    private RelativeLayout publisherVideoContainer;
    private OverlayView overlayView;
    private UnderlayView audioUnderlayView;
    private TrackMode trackMode = TrackMode.AUDIO_VIDEO;
    private boolean broadcasting;
    public static final int AUDIO_TRACK = 1;
    public static final int VIDEO_TRACK = 2;
    private static final String AUDIO_VIDEO_SDP = "v=0\r\no=OpenTok 0 0 IN IP4 127.0.0.1\r\ns=danger\r\nc=IN IP4 %s\r\nt=0 0\r\na=range:npt=now\r\na=tool:OpenTok Android RTP\r\n";
    private static final String VIDEO_ONLY_SDP = "m=video 0 RTP/AVP 97\r\nb=AS:64\r\na=rtpmap:97 H264/90000\r\na=fmtp:97 packetization-mode=1;sprop-parameter-sets=%s,%s\r\n";
    private static final String AUDIO_ONLY_SDP = "m=audio 8088 RTP/AVP 96\r\na=rtpmap:96 speex/16000/1\r\na=ptime:20\r\na=fmtp:96 mode=\"5\"\r\n";

    public EpicPublisher(Context context)
    {
        super(context);
        this.context = context;
        this.publisherVideoContainer = new RelativeLayout(this.context);

        this.publisherVideoContainer.setLayoutParams(new ViewGroup.LayoutParams(320, 240));

        this.overlayView = new OverlayView(this.context);
        this.publisherVideoContainer.addView(this.overlayView);

        this.audioUnderlayView = new UnderlayView(context, this.publisherView);
        this.audioUnderlayView.setVisibility(4);
        this.publisherVideoContainer.addView(this.audioUnderlayView);

        this.publisherView = new AspectRatioLayout(this.context, this.publisherVideoContainer, 1.333333333333333D);
        this.publisherView.setLayoutParams(new ViewGroup.LayoutParams(320, 240));

        this.publisherStats = new PublisherStatistics();
        this.publisherStats.setNetworkPublisherListener(this);

        this.broadcasting = false;
    }

    public void setSession(SessionImpl session)
    {
        this.session = session;
        RtspClientDriver rtspClientDriver = new RtspClientDriver();
        rtspClientDriver.setMode(RtspClientDriver.Mode.Publisher);
        rtspClientDriver.setHost(session.getSessionInfo().mediaServerHostname);

        rtspClientDriver.setPort(1935);

        rtspClientDriver.setPublisher(this);
        rtspClientDriver.setSession(session);
        rtspClientDriver.setStream(new StreamImpl(String.format("%d", new Object[] { Integer.valueOf((int)(Math.random() * 2147483647.0D)) }), this.name));

        rtspClientDriver.setDriverListener(this);
        String sdes = String.format("v=0\r\no=OpenTok 0 0 IN IP4 127.0.0.1\r\ns=danger\r\nc=IN IP4 %s\r\nt=0 0\r\na=range:npt=now\r\na=tool:OpenTok Android RTP\r\n", new Object[] { rtspClientDriver.getHost() });
        try {
            rtspClientDriver.setSessionDescription(SDPFactory.parseSessionDescription(sdes));
        }
        catch (SDPParseException e) {
            e.printStackTrace();
        }
        this.rtspClientDriver = rtspClientDriver;
    }

    public void publish()
    {
        if (!getUserPermissionsAccepted()) {
            displayPermissionsDialog();
            return;
        }

        this.publishRequestedTime = System.currentTimeMillis();

        Workers.getMainLoopExecutor().execute(new Runnable()
        {
            public void run() {
                EpicPublisher.this.attach();
            }
        });
    }

    public static Publisher newInstance(Context context)
    {
        return new EpicPublisher(context);
    }

    private void attach()
    {
        if ((FeatureCheck.allowPlaceholderTracks()) || (getPublishAudio()))
        {
            this.audioCaptureManager = new AndroidAudioCaptureManager(new SpeexNativeCodec());
            SpeexPacketizer speexPacketizer = new SpeexPacketizer(this.publisherStats);
            this.audioCaptureManager.setEncodedFrameListener(speexPacketizer);
            this.audioCaptureManager.setPublisherStatistics(this.publisherStats);

            SessionDescription audioDesc = getAudioTrackDescription();
            this.rtspClientDriver.addTrack(speexPacketizer, audioDesc);

            this.audioCaptureManager.setSilence(!getPublishAudio());

            this.audioCaptureManager.start();
        }

        if ((FeatureCheck.allowPlaceholderTracks()) || (getPublishVideo()))
        {
            if (this.camera != null) {
                this.camera.release();
                this.camera = null;
            }

            this.cameraId = (-1 == this.cameraId ? getFrontCameraId() : this.cameraId);
            try
            {
                this.camera = Camera.open(this.cameraId);
            }
            catch (Exception e)
            {
                if (null != this.delegate) {
                    this.delegate.onPublisherException(new OpentokException(OpentokException.Domain.PublisherErrorDomain, OpentokException.ErrorCode.CameraOpenAccess));

                    return;
                }
            }
            if (this.camera != null)
            {
                AndroidPipelineConfigurator.printAllConfigurations(this.camera);
                AndroidPipelineConfigurator.Config config = AndroidPipelineConfigurator.detectWorkableConfiguration(this.camera);

                if (null == config) {
                    throw new RuntimeException("Cannot find workable config for this device/camera");
                }

                try
                {
                    this.videoCaptureManager = new EpicVideoCaptureManager(this.context, this.camera, config);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                RelativeLayout.LayoutParams videoLayout = new RelativeLayout.LayoutParams(-1, -1);

                this.publisherVideoContainer.addView(this.videoCaptureManager.getView(), 0, videoLayout);
                this.videoCaptureManager.setListener(this);
                this.videoCaptureManager.setNetworkPublisher(this.publisherStats);

                this.avcEncoder = new AndroidAvcEncoder(config);
                this.avcEncoder.setParameterSetsListener(this);
                this.videoCaptureManager.setVideoConsumer(this.avcEncoder);

                this.avcPacketizer = new AvcPacketizer();
                this.avcPacketizer.setNetworkPublisher(this.publisherStats);

                this.avcEncoder.setFrameListener(this.avcPacketizer);
                this.avcEncoder.setNetworkPublisher(this.publisherStats);

                this.videoCaptureManager.setDataImageAudio(this.audioUnderlayView.getBitmapFromView());

                this.videoCaptureManager.setSilence(!getPublishVideo());

                this.videoCaptureManager.start();

                this.avcEncoder.start();
            }
        }
        else {
            Workers.getExecutorService().submit(new Runnable()
            {
                public void run() {
                    try {
                        EpicPublisher.this.rtspClientDriver.connect();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void destroy()
    {
        Workers.getExecutorService().submit(new Runnable()
        {
            public void run() {
                EpicPublisher.this.session.publisherWillClose(EpicPublisher.this);

                EpicPublisher.this.stopClientQos();
                EpicPublisher.this.audioCaptureManager.stop();
                EpicPublisher.this.videoCaptureManager.stop();
                EpicPublisher.this.rtspClientDriver.close();
                EpicPublisher.this.avcEncoder.close();
                EpicPublisher.this.broadcasting = false;

                ClientLogger.sendClientEvent(ClientLogger.generateComponentUnloadedReport(EpicPublisher.this));

                EpicPublisher.this.delegate.onPublisherStreamingStopped();
            }
        });
        Workers.submitToMainLoop(new Runnable()
        {
            public void run() {
                View thisView = EpicPublisher.this.getView();
                if (null == thisView)
                {
                    return;
                }
                ViewParent parent = thisView.getParent();
                if ((parent instanceof ViewGroup)) {
                    ViewGroup vgParent = (ViewGroup)parent;
                    vgParent.removeView(thisView);
                }
                thisView.setVisibility(8);
                if (null != parent)
                    parent.requestLayout();
            }
        });
    }

    private boolean getUserPermissionsAccepted()
    {
        SharedPreferences prefs = DeviceInfo.getApplicationContext().getSharedPreferences("permissions", 0);

        return prefs.getBoolean("opentok.publisher.accepted", false);
    }

    private void setUserPermissionsAccepted(boolean accepted) {
        SharedPreferences prefs = this.context.getApplicationContext().getSharedPreferences("permissions", 0);

        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.putBoolean("opentok.publisher.accepted", accepted);
        edit.commit();
    }

    private void displayPermissionsDialog() {
        final AlertDialog.Builder askCameraPermission = new AlertDialog.Builder(this.context);
        askCameraPermission.setTitle("Camera Permissions");
        askCameraPermission.setMessage("Would you like to allow this application to use your camera to stream live video?");

        askCameraPermission.setIcon(17301543);

        askCameraPermission.setPositiveButton("Allow", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                EpicPublisher.logger.info("User allows publisher permissions");

                EpicPublisher.this.setUserPermissionsAccepted(true);

                EpicPublisher.this.publish();
            }
        });
        askCameraPermission.setNegativeButton("Don't Allow", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                EpicPublisher.logger.info("User denies publisher permissions");

                EpicPublisher.this.setUserPermissionsAccepted(false);

                EpicPublisher.this.delegate.onPublisherException(new OpentokException(OpentokException.Domain.PublisherErrorDomain, OpentokException.ErrorCode.UserDeniedCameraAccess));
            }
        });
        Workers.getMainLoopExecutor().execute(new Runnable()
        {
            public void run() {
                askCameraPermission.show();
            }
        });
    }

    public boolean setCameraId(final int newCameraId)
    {
        if (this.cameraId == newCameraId) {
            logger.debug("call to setCameraId with unchanged id");
            return false;
        }

        this.cameraId = newCameraId;

        if (this.videoCaptureManager != null)
        {
            Workers.getExecutorService().submit(new Runnable()
            {
                public void run()
                {
                    EpicPublisher.this.videoCaptureManager.stop();

                    EpicPublisher.this.camera.release();
                    EpicPublisher.this.camera = Camera.open(newCameraId);

                    AndroidPipelineConfigurator.Config config = AndroidPipelineConfigurator.detectWorkableConfiguration(EpicPublisher.this.camera);

                    if (null == config) {
                        throw new RuntimeException("Cannot find workable config for this device/camera");
                    }

                    EpicPublisher.this.videoCaptureManager.setCamera(EpicPublisher.this.camera, config);

                    EpicPublisher.this.avcEncoder.restart();

                    EpicPublisher.this.delegate.onPublisherChangedCamera(newCameraId);
                }
            });
            return true;
        }
        return false;
    }

    public void swapCamera()
    {
        setCameraId((getCameraId() + 1) % Camera.getNumberOfCameras());
    }

    private int getFrontCameraId() {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == 1) {
                return i;
            }

        }

        return Camera.getNumberOfCameras() - 1;
    }

    public int getCameraId() {
        return this.cameraId;
    }

    public View getView() {
        return this.publisherView;
    }

    private SessionDescription getVideoTrackDescription(byte[] sps, byte[] pps)
    {
        try
        {
            String videoDescription = String.format("v=0\r\no=OpenTok 0 0 IN IP4 127.0.0.1\r\ns=danger\r\nc=IN IP4 %s\r\nt=0 0\r\na=range:npt=now\r\na=tool:OpenTok Android RTP\r\n", new Object[] { this.rtspClientDriver.getHost() });
            videoDescription = videoDescription + String.format("m=video 0 RTP/AVP 97\r\nb=AS:64\r\na=rtpmap:97 H264/90000\r\na=fmtp:97 packetization-mode=1;sprop-parameter-sets=%s,%s\r\n", new Object[] { Base64.encodeToString(sps, 2), Base64.encodeToString(pps, 2) });

            return SDPFactory.parseSessionDescription(videoDescription);
        }
        catch (SDPParseException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SessionDescription getAudioTrackDescription() {
        try {
            String audioDescription = String.format("v=0\r\no=OpenTok 0 0 IN IP4 127.0.0.1\r\ns=danger\r\nc=IN IP4 %s\r\nt=0 0\r\na=range:npt=now\r\na=tool:OpenTok Android RTP\r\n", new Object[] { this.rtspClientDriver.getHost() });
            audioDescription = audioDescription + "m=audio 8088 RTP/AVP 96\r\na=rtpmap:96 speex/16000/1\r\na=ptime:20\r\na=fmtp:96 mode=\"5\"\r\n";
            return SDPFactory.parseSessionDescription(audioDescription);
        }
        catch (SDPParseException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setRtspClientDriver(RtspClientDriver driver) {
        this.rtspClientDriver = driver;
    }

    public SessionImpl getSessionImpl()
    {
        return this.session;
    }

    public String getInstanceId()
    {
        return this.instanceId;
    }

    public String getMediaHostname()
    {
        return this.rtspClientDriver.getHost();
    }

    public long getUptimeMillis()
    {
        long now = System.currentTimeMillis();
        return now - this.streamingStartedTime;
    }

    public void avcParametersSetsEstablished(byte[] sps, byte[] pps)
    {
        SessionDescription videoDesc = getVideoTrackDescription(sps, pps);
        this.rtspClientDriver.addTrack(this.avcPacketizer, videoDesc);
        try
        {
            this.rtspClientDriver.connect();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onDriverStreamingStarted()
    {
        this.broadcasting = true;
        this.streamingStartedTime = System.currentTimeMillis();

        long loadPublisherTime = this.streamingStartedTime - this.publishRequestedTime;
        Map report = new HashMap();
        report.put("loadTimePublisher", String.valueOf(loadPublisherTime));
        ClientLogger.sendClientEvent(ClientLogger.generateClientEventReport("loadPublisherWidgetTime", this, report));

        logger.trace("time to load publisher widget " + loadPublisherTime);

        startClientQos();

        ClientLogger.sendClientEvent(ClientLogger.generateComponentLoadedReport(this));

        if (this.avcEncoder != null) {
            this.avcEncoder.restart();
        }

        if (null != this.delegate)
            this.delegate.onPublisherStreamingStarted();
    }

    public RtpConsumer onDriverNeedsRtpConsumerForCodec(String codecName)
    {
        return null;
    }

    public void onDriverFailedTrackResolution()
    {
        if (null != this.delegate)
            this.delegate.onPublisherException(new OpentokException(OpentokException.Domain.PublisherErrorDomain, OpentokException.ErrorCode.NoStreamMedia));
    }

    protected void startClientQos()
    {
        if (null == this.qosSender) {
            this.qosSender = new QosSender();
        }
        this.qosSenderFutureFirst = Workers.getScheduledExecutorService().schedule(this.qosSender, 10L, TimeUnit.SECONDS);
        this.qosSenderFuturePeriodic = Workers.getScheduledExecutorService().scheduleAtFixedRate(this.qosSender, 30L, 30L, TimeUnit.SECONDS);
    }

    protected void stopClientQos()
    {
        if (null != this.qosSenderFutureFirst) {
            this.qosSenderFutureFirst.cancel(true);
        }
        if (null != this.qosSenderFuturePeriodic)
            this.qosSenderFuturePeriodic.cancel(true);
    }

    public void onDriverFailed()
    {
        this.delegate.onPublisherException(new OpentokException(OpentokException.Domain.PublisherErrorDomain, OpentokException.ErrorCode.FailedMediaPackets));
        destroy();
    }

    public String getWidgetType()
    {
        return "Publisher";
    }

    public void onVideoCapturePreviewSurfaceDestroyed()
    {
        destroy();
    }

    public boolean getPublishVideo()
    {
        return (this.trackMode.getMode() & 0x2) > 0;
    }

    public boolean getPublishAudio()
    {
        return (this.trackMode.getMode() & 0x1) > 0;
    }

    public void setPublishVideo(boolean publishVideo)
    {
        if (publishVideo) {
            this.trackMode = TrackMode.fromTypeCode(this.trackMode.getMode() | 0x2);
            this.audioUnderlayView.setVisibility(4);
        } else {
            this.trackMode = TrackMode.fromTypeCode(this.trackMode.getMode() & 0xFFFFFFFD);
            this.audioUnderlayView.setVisibility(0);
        }

        if (null != this.videoCaptureManager)
            this.videoCaptureManager.setSilence(!getPublishVideo());
    }

    public void setPublishAudio(boolean publishAudio)
    {
        if (publishAudio)
            this.trackMode = TrackMode.fromTypeCode(this.trackMode.getMode() | 0x1);
        else {
            this.trackMode = TrackMode.fromTypeCode(this.trackMode.getMode() & 0xFFFFFFFE);
        }

        if (null != this.audioCaptureManager)
            this.audioCaptureManager.setSilence(!getPublishAudio());
    }

    public PublisherStatistics getStatistics()
    {
        return this.publisherStats;
    }

    public void onOutAudioBitsChange(double outAudioBitsEMA) {
        this.publisherStats.setOutAudioBitsPerSecondEMA(outAudioBitsEMA);
    }

    public void onOutVideoBitsChange(double outVideoBitsEMA)
    {
        this.publisherStats.setOutVideoBitsPerSecondEMA(outVideoBitsEMA);
    }

    public void onVideoFpsChange(double videoFpsEMA)
    {
        this.publisherStats.setVideoFpsEMA(videoFpsEMA);
    }

    public void onVideoDroppedFpsChange(double videoDroppedFpsEMA)
    {
        this.publisherStats.setVideoDroppedFpsEMA(videoDroppedFpsEMA);
    }

    public String getStreamId()
    {
        return this.rtspClientDriver.getStream().getStreamId();
    }

    public void setName(String name)
    {
        if ((null != name) && (name.length() > 1000)) {
            logger.warn("setName: name length exceeded");
            this.delegate.onPublisherException(new OpentokException(OpentokException.Domain.PublisherErrorDomain, OpentokException.ErrorCode.MaxNameLengthExceeded));
            name = name.substring(0, 999);
        }

        if (this.broadcasting) {
            logger.warn("Cannot set the name of publisher that is currently publishing");
            return;
        }

        this.name = name;
        if (this.rtspClientDriver != null) {
            StreamImpl rtspClientStream = (StreamImpl)this.rtspClientDriver.getStream();
            rtspClientStream.setName(name);
        }
    }

    private class QosSender
            implements Runnable
    {
        private QosSender()
        {
        }

        public void run()
        {
            Map report = ClientLogger.generateQosReport(EpicPublisher.this);
            EpicPublisher.logger.trace(String.format("publisher qos report: %s", new Object[] { report }));
            ClientLogger.sendQoS(report);
        }
    }

    private static enum TrackMode
    {
        NO_TRACKS(0),
        AUDIO_ONLY(1),
        VIDEO_ONLY(2),
        AUDIO_VIDEO(3);

        private int bits;

        private TrackMode(int bits) { this.bits = bits; }

        public int getMode() {
            return this.bits;
        }
        public static TrackMode fromTypeCode(int id) {
            for (TrackMode mode : values()) {
                if (mode.getMode() == id) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown type code " + id);
        }
    }
}