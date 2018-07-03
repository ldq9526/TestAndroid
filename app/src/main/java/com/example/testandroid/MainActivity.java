package com.example.testandroid;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback {
    private static final int CAMERA_CLOSED = 0;
    private static final int CAMERA_FRONT = 1;
    private static final int CAMERA_BACK = 2;


    private Bitmap bitmap = null;

    private int cameraState = CAMERA_CLOSED;
    private Camera camera = null;
    private Camera.Size previewSize;
    private SurfaceHolder surfaceHolder;

    private SurfaceView surfaceView;
    private ImageView imageView;
    private Button button;

    static {
        System.loadLibrary("native-lib");
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    private void initializeScreen() {

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.hide();
        setContentView(R.layout.main_layout);
    }

    private void initializeView() {
        surfaceView = (SurfaceView)findViewById(R.id.surface_view);
        imageView = (ImageView)findViewById(R.id.image_view);
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageBitmap(null);
                if(cameraState == CAMERA_CLOSED) return;
                if(cameraState == CAMERA_FRONT) {
                    if(!switchToBackCamera()) {
                        Toast.makeText(MainActivity.this,"Fail to switch !", Toast.LENGTH_SHORT).show();
                        if(!openCamera()) {
                            Toast.makeText(MainActivity.this,"Fail to open front camera !", Toast.LENGTH_SHORT).show();
                        } else {
                            setCameraParameters();
                            startCameraPreview();
                        }
                    }
                } else {
                    if(!openCamera()) {
                        Toast.makeText(MainActivity.this,"Fail to switch !", Toast.LENGTH_SHORT).show();
                    } else {
                        setCameraParameters();
                        startCameraPreview();
                    }
                }
            }
        });
    }

    /* default : open front camera. return true if succeed opening camera */
    private boolean openCamera() {
        if(camera != null)
            closeCamera();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                camera = Camera.open(camIdx);
                cameraState = CAMERA_FRONT;
                break;
            }
        }
        if(camera == null) {
            camera = Camera.open();
            cameraState = CAMERA_BACK;
        }
        if(camera == null) {
            cameraState = CAMERA_CLOSED;
            return false;
        } else {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                return true;
            } catch (Exception e) {
                if (null != camera) {
                    camera.release();
                    camera = null;
                }
                cameraState = CAMERA_CLOSED;
                return false;
            }
        }
    }

    private boolean switchToBackCamera() {
        if(camera != null && cameraState == CAMERA_FRONT)
            closeCamera();
        camera = Camera.open();
        cameraState = CAMERA_BACK;
        if(camera == null) {
            cameraState = CAMERA_CLOSED;
            return false;
        } else {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                setCameraParameters();
                startCameraPreview();
                return true;
            } catch (Exception e) {
                if (null != camera) {
                    camera.release();
                    camera = null;
                }
                cameraState = CAMERA_CLOSED;
                return false;
            }
        }
    }

    private void setCameraParameters() {
        if(camera == null) return;
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if(supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes,960,540);
            parameters.setPreviewSize(previewSize.width,previewSize.height);
        } else {
            parameters.setPreviewSize(960, 540);
        }
        parameters.setPictureFormat(ImageFormat.NV21);
        camera.setParameters(parameters);

    }

    private void startCameraPreview() {
        if(camera == null) return;
        camera.setPreviewCallback(this);
        camera.startPreview();
    }

    private void closeCamera() {
        if(camera == null) return;
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
        cameraState = CAMERA_CLOSED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeScreen();
        initializeView();
        initializeFaceDetector();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!openCamera()) {
            Toast.makeText(MainActivity.this, "Fail to open camera", Toast.LENGTH_SHORT).show();
        } else {
            setCameraParameters();
            startCameraPreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != camera) {
            closeCamera();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(!openCamera()) {
            Toast.makeText(MainActivity.this, "Fail to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setCameraParameters();
        startCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        closeCamera();
    }

    private native void initializeFaceDetector();
    private native void detectFace(byte[] data, int width, int height, Bitmap bitmap);

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(bitmap == null)
            bitmap = Bitmap.createBitmap(previewSize.width,previewSize.height, Bitmap.Config.ARGB_8888);
        detectFace(data,previewSize.width,previewSize.height,bitmap);
        imageView.setImageBitmap(bitmap);
    }
}