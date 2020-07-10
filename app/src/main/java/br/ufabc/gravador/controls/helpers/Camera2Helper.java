package br.ufabc.gravador.controls.helpers;


import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Camera2Helper {

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private final Context context;
    private final Handler mBackgroundHandler;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    //mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mRecordCaptureSession;
    private boolean isRecording = false, isPreviewing = false;
    private boolean requiresSnapshots = false;
    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mImageSize;
    private String mCameraId;
    private int mTotalRotation;

    private Surface recordSurface;

    private ImageReader mImageReader;

    public Camera2Helper(Context context, Handler backgroundHandler) {
        this.context = context;
        this.mBackgroundHandler = backgroundHandler;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0)
            return Collections.min(bigEnough, (Size lhs, Size rhs) -> Integer.signum(
                    lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight()));
        else return choices[0];
    }

    public CameraDimensions setupCamera(boolean facingFront, boolean requireSnapshots, CameraReadyCallback callback) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (facingFront) {
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                            CameraCharacteristics.LENS_FACING_BACK) // if looking for front, ignore back
                        continue;
                } else {
                    if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                            CameraCharacteristics.LENS_FACING_FRONT)
                        continue;
                }

                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

                Point size = new Point();
                windowManager.getDefaultDisplay().getRealSize(size);

                int deviceOrientation = ORIENTATIONS.get(windowManager.getDefaultDisplay().getRotation());
                int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mTotalRotation = (sensorOrienatation + deviceOrientation + 360) % 360;

                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = swapRotation ? size.y : size.x;
                int rotatedHeight = swapRotation ? size.x : size.y;

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                mCameraId = cameraId;
                cameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        if (callback != null) callback.onCameraReady();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        stopCaptureSession();
                        camera.close();
                        mCameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        stopCaptureSession();
                        camera.close();
                        mCameraDevice = null;
                    }
                }, mBackgroundHandler);
                return new CameraDimensions(rotatedHeight, rotatedWidth, mTotalRotation);
            }
        } catch (SecurityException | CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startRecording(Surface previewSurface, Surface recordSurface) {
        stopCaptureSession();
        isRecording = true;
        startCaptureSession(previewSurface, recordSurface, false);
    }

    public void startPreviewing(Surface previewSurface) {
        stopCaptureSession();
        isPreviewing = true;
        startCaptureSession(previewSurface, null, false);
    }

    private void startCaptureSession(Surface previewSurface, Surface recordSurface, boolean keepSession) {

        this.recordSurface = recordSurface;

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(
                    isRecording ?
                            CameraDevice.TEMPLATE_RECORD :
                            isPreviewing ?
                                    CameraDevice.TEMPLATE_PREVIEW :
                                    CameraDevice.TEMPLATE_STILL_CAPTURE //SHOULD NOT HAPPEN
            );
            if (previewSurface != null) mCaptureRequestBuilder.addTarget(previewSurface);
            if (recordSurface != null) mCaptureRequestBuilder.addTarget(recordSurface);

            List<Surface> surfaces = new ArrayList<>();
            if (previewSurface != null) surfaces.add(previewSurface);
            if (recordSurface != null) surfaces.add(recordSurface);
            surfaces.add(mImageReader.getSurface());

            if (keepSession)
                mRecordCaptureSession.setRepeatingRequest(
                        mCaptureRequestBuilder.build(), null, null);
            else
                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        mRecordCaptureSession = session;
                        try {
                            mRecordCaptureSession.setRepeatingRequest(
                                    mCaptureRequestBuilder.build(), null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        Log.d("startRecordSession", "onConfigureFailed");
                    }
                }, null);

        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
            isPreviewing = false;
        }
    }

    public void changePreviewSurface(Surface previewSurface) {
        if (isRecording)
            startCaptureSession(previewSurface, recordSurface, previewSurface == null);
        else if (isPreviewing) {
            if (previewSurface == null) stopCaptureSession();
            else startCaptureSession(previewSurface, recordSurface, false);
        }
    }

    public void stopCaptureSession() {
        if (mRecordCaptureSession != null) {
            mRecordCaptureSession.close();
            mRecordCaptureSession = null;
        }
        isRecording = false;
        isPreviewing = false;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPreviewing() {
        return isPreviewing;
    }

    public interface CameraReadyCallback {
        void onCameraReady();
    }

    public static class CameraDimensions {
        public final int h, w, d;

        public CameraDimensions(int h, int w, int d) {
            this.h = h;
            this.w = w;
            this.d = d;
        }
    }
}
