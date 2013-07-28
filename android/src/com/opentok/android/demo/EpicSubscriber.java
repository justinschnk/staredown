package com.opentok.android.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import com.opentok.android.OpentokException;
import com.opentok.android.Stream;
import com.opentok.client.FeatureCheck;
import com.opentok.impl.SubscriberImpl;
import com.opentok.media.AbstractCodec;
import com.opentok.media.AndroidSurfaceRenderer;
import com.opentok.media.avc.AvcDecoder;
import com.opentok.media.avc.AvcSoftDecoder;
import com.opentok.rtp.RtpConsumer;
import com.opentok.rtp.codec.AvcDepacketizer;
import com.opentok.stat.SubscriberStatistics;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;


public class EpicSubscriber extends SubscriberImpl {

    private AvcDepacketizer avcDepacketizer;
    private SubscriberStatistics networkSubscriber;
    private SurfaceView videoView;
    private Context context;
    public Bitmap bitmap = null;

    public EpicSubscriber(Context context, Stream stream) {
        super(context, stream);
        this.context = context;
    }

    public Object getField(Class clazz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            makeAccessible(f);
            return f.get(this);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }
    public void makeAccessible(Field field) {
        if (!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()))
        {
            field.setAccessible(true);
        }
    }

    public static EpicSubscriber newInstance(Context context, Stream stream)
    {
        return new EpicSubscriber(context, stream);
    }

    public interface DrawCallback {
        public void callback(Bitmap b);
    }

    public DrawCallback callback;

    public RtpConsumer onDriverNeedsRtpConsumerForCodec(String codecName)
    {
        RtpConsumer c = super.onDriverNeedsRtpConsumerForCodec(codecName);
        if ((codecName.equals("H264")) && ((FeatureCheck.allowPlaceholderTracks()) || (getSubscribeToVideo()))) {

            try {
            Class subscriberClass = SubscriberImpl.class;

            Field f = subscriberClass.getDeclaredField("avcDepacketizer");
            f.setAccessible(true);

            Field f2 = subscriberClass.getDeclaredField("networkSubscriber");
            f2.setAccessible(true);

            Field f3 = subscriberClass.getDeclaredField("videoView");
            f3.setAccessible(true);

            final Field f4 = AndroidSurfaceRenderer.class.getDeclaredField("drawingBitmap");
            f4.setAccessible(true);

            final AvcDepacketizer avcDepacketizer = (AvcDepacketizer) f.get(this);
            final SubscriberStatistics networkSubscriber = (SubscriberStatistics) f2.get(this);
            final SurfaceView videoView = (SurfaceView) f3.get(this);

            final AvcDecoder decoder = avcDepacketizer.getAvcDecoder();
            final AndroidSurfaceRenderer renderer = new AndroidSurfaceRenderer(videoView.getHolder());
            decoder.attachDecoderMonitor(new AbstractCodec.DecoderMonitor()
            {
                public void onDecoderCanPoll() {
                    AvcDecoder.DecoderOutput output = decoder.pollDecoder();

                    if (null != output) {
                        renderer.drawPixels(output.pixelData, output.width, output.height);
                        try {
                            if (callback != null) callback.callback((Bitmap) f4.get(renderer));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        networkSubscriber.setVideoResolutionHeight(output.height);
                        networkSubscriber.setVideoResolutionWidth(output.width);
                        avcDepacketizer.getNetworkSubscriber().didRenderFrame();
                    }
                    else {
                        avcDepacketizer.getNetworkSubscriber().didDropFrame();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        return c;
    }
}
