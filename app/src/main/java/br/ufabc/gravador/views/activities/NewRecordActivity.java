package br.ufabc.gravador.views.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;

import br.ufabc.gravador.R;

public class NewRecordActivity extends AbstractMenuActivity {

    int REQUEST_CAMERA = 1001;
    int REQUEST_MICROPHONE = 1002;
    String[] MICROPHONE_PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    String[] CAMERA_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Button recordAudio, recordVideo;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_new_record, R.id.my_toolbar, true);

        if ( !this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) )
            recordVideo.setEnabled(false);

        recordAudio = findViewById(R.id.recordAudio);
        recordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                recordAudioOnClick(view);
            }
        });

        recordVideo = findViewById(R.id.recordVideo);
        recordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                recordVideoOnClick(view);
            }
        });
    }

    void recordAudioOnClick ( View view ) {
        boolean allowed = true;
        for ( String permission : MICROPHONE_PERMISSIONS ) {
            allowed = ActivityCompat.checkSelfPermission(this,
                    permission) == PackageManager.PERMISSION_GRANTED;
            Log.i("Permission", permission + " : " + allowed);
            if ( !allowed ) break;
        }
        if ( allowed ) {
            Intent intent = new Intent(this, RecordAudioActivity.class);
            startActivity(intent);
        } else ActivityCompat.requestPermissions(this, MICROPHONE_PERMISSIONS, REQUEST_MICROPHONE);
    }

    void recordVideoOnClick ( View view ) {
        boolean allowed = true;
        for ( String permission : CAMERA_PERMISSIONS ) {
            if ( ActivityCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED ) {
                allowed = false;
                break;
            }
        }
        if ( !allowed ) ActivityCompat.requestPermissions(this, CAMERA_PERMISSIONS, REQUEST_CAMERA);
        else {
            Intent intent = new Intent(this, VideoRecordActivity.class);
            startActivity(intent);
        }
    }
}
