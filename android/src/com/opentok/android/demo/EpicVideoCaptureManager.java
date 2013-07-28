package com.opentok.android.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import com.opentok.capture.VideoCaptureManager;
import com.opentok.media.AndroidPipelineConfigurator;
import com.opentok.media.AndroidPipelineConfigurator.Config;
import com.opentok.media.Codec;
import com.opentok.runtime.Workers;
import com.opentok.stat.PublisherStatistics;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import tokbox.org.slf4j.Logger;
import tokbox.org.slf4j.LoggerFactory;

public class EpicVideoCaptureManager
        implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private static Logger log = LoggerFactory.getLogger("opentok-capture");
    private Camera camera;
    private AtomicBoolean previewActive = new AtomicBoolean(false);
    private Codec<byte[], ?, ?, ?> videoConsumer;
    private VideoCaptureManager.Listener listener;
    public static final int CAMERA_WIDTH = 320;
    public static final int CAMERA_HEIGHT = 240;
    public static final int CAMERA_MIN_FPS = 10;
    public static final int CAMERA_MAX_FPS = 15;
    private int previewBufferSize = 0;
    public PublisherStatistics networkPublisher;
    private SurfaceView surfaceView;
    private boolean silent;
    private Future<?> silenceGenerator;
    byte[] dataImageAudio;

    public EpicVideoCaptureManager(Context context, Camera camera, AndroidPipelineConfigurator.Config config)
            throws IOException
    {
        this.surfaceView = new SurfaceView(context);

        SurfaceHolder surfaceHolder = this.surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        setCamera(camera, config);
    }

    public void setSilence(boolean shh)
    {
        if (this.silent == shh)
        {
            return;
        }

        this.silent = shh;

        if (this.silent) {
            stop();
            this.silenceGenerator = Workers.getScheduledExecutorService().scheduleAtFixedRate(new Runnable()
            {
                public void run() {
                    try {
                        if (null != EpicVideoCaptureManager.this.videoConsumer)
                            EpicVideoCaptureManager.this.videoConsumer.offerEncoder(EpicVideoCaptureManager.this.dataImageAudio);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
                    , 1000L, 200L, TimeUnit.MILLISECONDS);
        }
        else
        {
            if (null != this.silenceGenerator) {
                this.silenceGenerator.cancel(true);
                this.silenceGenerator = null;
            }
            start();
        }
    }

    public void start() {
        if ((null != this.camera) && (this.previewActive.compareAndSet(false, true)))
            this.camera.startPreview();
    }

    public void stop()
    {
        if ((null != this.camera) && (this.previewActive.compareAndSet(true, false)))
            this.camera.stopPreview();
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            this.camera.setPreviewDisplay(holder);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (null == this.camera)
            return;
        try
        {
            this.camera.setPreviewDisplay(holder);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        this.camera.stopPreview();
        holder.removeCallback(this);
        releaseCamera();
        Workers.getScheduledExecutorService().submit(new Runnable()
        {
            public void run() {
                if (null != EpicVideoCaptureManager.this.listener)
                    EpicVideoCaptureManager.this.listener.onVideoCapturePreviewSurfaceDestroyed();
            }
        });
    }

    public interface DrawCallback {
        public void callback(Bitmap b);
    }

    public static DrawCallback drawcallback;

    /**
     * Converts YUV420 NV21 to ARGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a ARGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    public static int[] decodeYUV(byte [] data, int width, int height) {
        int size = width*height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        // i along Y and the final pixels
        // k along pixels U and V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k  ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;

            pixels[i  ] = convertYUVtoARGB(y1, u, v);
            pixels[i+1] = convertYUVtoARGB(y2, u, v);
            pixels[width+i  ] = convertYUVtoARGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoARGB(y4, u, v);

            if (i!=0 && (i+2)%width==0)
                i+=width;
        }

        return pixels;
    }

    private static int convertYUVtoARGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)1.402f*u;
        g = y - (int)(0.344f*v +0.714f*u);
        b = y + (int)1.772f*v;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (r<<16) | (g<<8) | b;
    }

    public void onPreviewFrame(final byte[] data, final Camera camera)
    {
        Log.d("asdfdfsaafsdafdsafsdafsdaffdasfadsadfs", camera.getParameters().getPreviewFormat() + "");
        int[] rgb = decodeYUV(data, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height);

        if(drawcallback != null) drawcallback.callback(Bitmap.createBitmap(rgb, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, Bitmap.Config.RGB_565));
        Workers.getScheduledExecutorService().submit(new Runnable()
        {
            public void run()
            {
                if (EpicVideoCaptureManager.this.previewBufferSize != data.length) {
                    EpicVideoCaptureManager.this.previewBufferSize = data.length;
                    EpicVideoCaptureManager.this.networkPublisher.setVideoResolutionHeight(camera.getParameters().getPreviewSize().height);
                    EpicVideoCaptureManager.this.networkPublisher.setVideoResolutionWidth(camera.getParameters().getPreviewSize().width);
                }

                if (null != EpicVideoCaptureManager.this.videoConsumer) {
                    EpicVideoCaptureManager.this.videoConsumer.offerEncoder(data);
                }

                camera.addCallbackBuffer(data);

                EpicVideoCaptureManager.this.networkPublisher.didTotalCapturedFrames();
            }
        });
    }

    public void setVideoConsumer(Codec<byte[], ?, ?, ?> videoConsumer) {
        this.videoConsumer = videoConsumer;
    }

    public VideoCaptureManager.Listener getListener() {
        return this.listener;
    }

    public void setListener(VideoCaptureManager.Listener listener) {
        this.listener = listener;
    }

    public void setNetworkPublisher(PublisherStatistics networkPublisher) {
        this.networkPublisher = networkPublisher;
    }

    private void releaseCamera() {
        this.camera.stopPreview();
        this.camera.setPreviewCallback(null);
        this.camera.release();
        this.camera = null;
    }

    public void setCamera(Camera newCamera, AndroidPipelineConfigurator.Config config) {
        boolean restart = false;

//        if (!this.previewActive2.get())
//        {
            restart = true;
//        }

        newCamera.lock();
        Camera.Parameters params = newCamera.getParameters();
        params.setPreviewSize(320, 240);
        params.setPreviewFormat(842094169);
        params.setPreviewFormat(config.cameraFormat);
        newCamera.setParameters(params);
        int previewBufferSize = ImageFormat.getBitsPerPixel(params.getPreviewFormat()) * params.getPreviewSize().height * params.getPreviewSize().width;

        newCamera.addCallbackBuffer(new byte[previewBufferSize / 8]);
        newCamera.setPreviewCallbackWithBuffer(this);
        try
        {
            newCamera.setPreviewDisplay(this.surfaceView.getHolder());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        this.camera = newCamera;
        if (restart)
            start();
    }

    public View getView()
    {
        return this.surfaceView;
    }

    public byte[] getDataImageAudio() {
        return this.dataImageAudio;
    }

    public void setDataImageAudio(byte[] dataImageAudio) {
        this.dataImageAudio = dataImageAudio;
    }

}