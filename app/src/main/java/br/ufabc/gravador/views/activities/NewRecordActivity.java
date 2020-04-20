package br.ufabc.gravador.views.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.PermissionHelper;

public class NewRecordActivity extends AbstractMenuActivity {

    public static int AUDIO_REQUEST = 1111, VIDEO_REQUEST = 2222;

    Button recordAudio, recordVideo;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_new_record;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {

        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            recordVideo.setEnabled(false);

        recordAudio = findViewById(R.id.recordAudio);
        recordAudio.setOnClickListener(this::recordAudioOnClick);

        recordVideo = findViewById(R.id.recordVideo);
        recordVideo.setOnClickListener(this::recordVideoOnClick);
    }

    void recordAudioOnClick ( View view ) {
        PermissionHelper ph = new PermissionHelper(this);
        Intent intent = new Intent(this, RecordAudioActivity.class);
        ph.startIfPermitted(intent, PermissionHelper.REQUEST_MICROPHONE,
                PermissionHelper.MICROPHONE_PERMISSIONS);
    }

    void recordVideoOnClick ( View view ) {
        PermissionHelper ph = new PermissionHelper(this);
        Intent intent = new Intent(this, RecordVideoActivity.class);
        ph.startIfPermitted(intent, PermissionHelper.REQUEST_CAMERA,
                PermissionHelper.CAMERA_PERMISSIONS);
    }
}
