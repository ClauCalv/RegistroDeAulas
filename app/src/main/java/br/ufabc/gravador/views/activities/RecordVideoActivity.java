package br.ufabc.gravador.views.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;

@SuppressWarnings("deprecation")
public class RecordVideoActivity extends AbstractServiceActivity {

    private final String begin = "Gravação não iniciada", recording = "Gravação em andamento",
            end = "Gravação Encerrada"; //TODO HARDCODED
    protected ViewGroup videoLayout;
    protected TextureView videoSurface;
    protected ImageButton muteVideo, flipCamera, goFullscreen, recordVideo;
    protected TextView videoInfo, videoTime;
    protected boolean isFullscreen = false;
    protected boolean isRecording = false;
    protected Gravacao gravacao;
    private boolean facingFront = false;
    private int cameraRotationOffset;
    private Camera camera;
    private MediaRecorder mediaRecorder = new MediaRecorder();
    private boolean useAudio = true;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_record_video;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) finish();

        videoLayout = findViewById(R.id.video_main_layout);

        videoSurface = findViewById(R.id.video_surface);

        muteVideo = findViewById(R.id.video_mute);
        muteVideo.setOnClickListener(this::muteVideoOnClick);
        muteVideo.setImageResource(useAudio ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);

        flipCamera = findViewById(R.id.video_flip_camera);
        flipCamera.setOnClickListener(this::flipCameraOnClick);

        goFullscreen = findViewById(R.id.video_toggle_fullscreen);
        goFullscreen.setOnClickListener(this::goFullscreenOnClick);

        recordVideo = findViewById(R.id.video_record);
        recordVideo.setOnClickListener(this::recordVideoOnClick);

        videoInfo = findViewById(R.id.video_info);

        videoTime = findViewById(R.id.video_time);
        videoTime.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onServiceOnline() {
        if (!gravacaoService.hasGravacao()) {
            gravacao = gravacaoService.createNewGravacao();
        } else gravacao = gravacaoService.getGravacao();
//        fragment.updateGravacao();

        gravacaoService.setTimeUpdateListener(new GravacaoService.TimeUpdateListener() {
            @Override
            public void onTimeUpdate(long time) {
                if (videoTime != null)
                    videoTime.setText(Gravacao.formatTime(time));
            }
        });
        updateState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPreview();
    }

    private void updateState() {
        if (!isBound) return;

        int serviceStatus = gravacaoService.getServiceStatus();

        videoInfo.setText(
                serviceStatus == GravacaoService.STATUS_RECORDING ? recording :
                        serviceStatus == GravacaoService.STATUS_WAITING_SAVE ? end :
                                begin);
        videoTime.setVisibility(
                serviceStatus == GravacaoService.STATUS_WAITING_SAVE ||
                        serviceStatus == GravacaoService.STATUS_RECORDING ? View.VISIBLE :
                        View.INVISIBLE);

        flipCamera.setEnabled(
                serviceStatus == GravacaoService.STATUS_WAITING_SAVE ||
                        serviceStatus == GravacaoService.STATUS_RECORDING ? false : true);

        muteVideo.setEnabled(
                serviceStatus == GravacaoService.STATUS_WAITING_SAVE ||
                        serviceStatus == GravacaoService.STATUS_RECORDING ? false : true);


    }

    private void recordVideoOnClick(View view) {
        if (!isBound) return;

        if (gravacaoService.getServiceStatus() == GravacaoService.STATUS_IDLE)
            gravacaoService.prepareForRecord(GravacaoService.MEDIATYPE_VIDEO);
        switch (gravacaoService.getServiceStatus()) {
            case GravacaoService.STATUS_RECORD_PREPARED:
                gravacaoService.setVideoParameters(camera, useAudio);
                camera.unlock();
                if (!gravacaoService.startRecording()) {
                    camera.lock();
                    Toast.makeText(this, "Falha em iniciar gravação",
                            Toast.LENGTH_LONG).show(); //TODO hardcoded
                    gravacaoService.goIdle();
                }
                break;
            case GravacaoService.STATUS_RECORDING:
                gravacaoService.stopRecording();
                camera.lock();
                break;
            case GravacaoService.STATUS_WAITING_SAVE:
                onAnnotationSaved();
                break;
        }

        updateState();
    }

    private void goFullscreenOnClick(View view) {
    }

    private void flipCameraOnClick(View view) {
        facingFront = !facingFront;
        try {
            SurfaceTexture surface = videoSurface.getSurfaceTexture();
            releaseCamera();
            camera = findCamera();
            fixRotation();
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ;
    }

    private void muteVideoOnClick(View view) {
        useAudio = !useAudio;
        muteVideo.setImageResource(useAudio ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
    }

    private Camera findCamera() {
        try {
            int n = Camera.getNumberOfCameras();
            for (int i = 0; i < n; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == (facingFront ?
                        Camera.CameraInfo.CAMERA_FACING_FRONT :
                        Camera.CameraInfo.CAMERA_FACING_BACK)) {
                    cameraRotationOffset = info.orientation;
                    return Camera.open(i);
                }
            }
            return Camera.open();
        } catch (Exception e) {
            return null;
        }
    }

    private void fixRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        int displayRotation;
        if (facingFront) {
            displayRotation = (cameraRotationOffset + degrees) % 360;
            displayRotation = (360 - displayRotation) % 360; // compensate mirroring
        } else {
            displayRotation = (cameraRotationOffset - degrees + 360) % 360;
        }

        Log.v("RotationCalc", "rotation cam / phone = displayRotation: " + cameraRotationOffset + " / " + degrees + " = "
                + displayRotation);

        camera.setDisplayOrientation(displayRotation);

        int rotate;
        if (facingFront) {
            rotate = (360 + cameraRotationOffset + degrees) % 360;
        } else {
            rotate = (360 + cameraRotationOffset - degrees) % 360;
        }

        Log.v("RotationCalc", "screenshot rotation: " + cameraRotationOffset + " / " + degrees + " = " + rotate);

        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(rotate);
        camera.setParameters(parameters);
    }

    public void startPreview() {

        if (camera == null)
            camera = findCamera();

        fixRotation();

        videoSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    camera.setPreviewTexture(surface);
                    camera.startPreview();
                } catch (IOException e) {
                    Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
                }
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                }
                return true;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            gravacaoService.scheduleCameraRelease();
            camera = null;
        } else releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    public void onAnnotationSaved() {
        Intent intent = new Intent(this,
                NameToSaveActivity.class);
        intent.putExtra("RequestCode", NewRecordActivity.VIDEO_REQUEST);
        startActivityForResult(intent, NewRecordActivity.VIDEO_REQUEST);
    }

    @Override
    public void onBackPressed() {
        if (gravacao == null || !gravacao.isLastSaved())
            new AlertDialog.Builder(this).setMessage(
                    "Descartar tudo? Não poderá desfazer esta ação")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setNegativeButton("Não", null)
                    .show();
        else finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NewRecordActivity.VIDEO_REQUEST)
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
    }
}
