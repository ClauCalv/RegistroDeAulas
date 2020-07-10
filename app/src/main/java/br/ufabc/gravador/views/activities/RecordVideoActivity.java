package br.ufabc.gravador.views.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.ImageWriter;
import android.media.MediaRecorder;
import android.os.Bundle;
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


public class RecordVideoActivity extends AbstractServiceActivity {

    private final String begin = "Gravação não iniciada", recording = "Gravação em andamento",
            end = "Gravação Encerrada"; //TODO HARDCODED
    protected ViewGroup videoLayout;
    protected TextureView videoSurface;
    protected ImageWriter mImageWriter;
    protected ImageButton muteVideo, flipCamera, goFullscreen, recordVideo;
    protected TextView videoInfo, videoTime;
    protected boolean isFullscreen = false;
    protected boolean isRecording = false;
    protected Gravacao gravacao;
    private boolean facingFront = false;
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

        if (videoSurface.isAvailable()) {
            mImageWriter = ImageWriter.newInstance(new Surface(videoSurface.getSurfaceTexture()), 2);
            gravacaoService.setPreviewWriter(mImageWriter);
        }
        videoSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mImageWriter = ImageWriter.newInstance(new Surface(surface), 2);
                gravacaoService.setPreviewWriter(mImageWriter);
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                gravacaoService.abortPreviewWriter(mImageWriter);
                if (mImageWriter != null) {
                    mImageWriter.close();
                    mImageWriter = null;
                }
                return true;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }
        });

        updateState();
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
            gravacaoService.prepareForRecord(GravacaoService.MEDIATYPE_VIDEO, facingFront);
        switch (gravacaoService.getServiceStatus()) {
            case GravacaoService.STATUS_RECORD_PREPARED:
                if (!gravacaoService.startRecording()) {
                    Toast.makeText(this, "Falha em iniciar gravação",
                            Toast.LENGTH_LONG).show(); //TODO hardcoded
                    gravacaoService.goIdle();
                }
                break;
            case GravacaoService.STATUS_RECORDING:
                gravacaoService.stopRecording();
                break;
            case GravacaoService.STATUS_WAITING_SAVE:
                saveAnnotation();
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
    }

    private void muteVideoOnClick(View view) {
        if (gravacaoService != null) return;

        useAudio = gravacaoService.invertMicrophoneMute();
        muteVideo.setImageResource(useAudio ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
    }

    public void saveAnnotation() {
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
