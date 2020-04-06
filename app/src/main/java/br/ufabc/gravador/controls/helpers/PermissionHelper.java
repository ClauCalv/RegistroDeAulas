package br.ufabc.gravador.controls.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class PermissionHelper {

    public static final String[] MICROPHONE_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static final String[] STREAM_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    public static final int REQUEST_CAMERA = 1001, REQUEST_MICROPHONE = 1002, REQUEST_STREAM = 1003;

    private Activity activity;

    public PermissionHelper ( Activity activity ) {
        this.activity = activity;
    }

    public boolean startIfPermitted ( Intent intent, int requestCode, String... permissions ) {
        boolean allowed = true;
        for ( String permission : permissions ) {
            allowed = ActivityCompat.checkSelfPermission(activity,
                    permission) == PackageManager.PERMISSION_GRANTED;
            Log.i("Permission", permission + " : " + allowed);
            if ( !allowed ) break;
        }
        if ( allowed ) {
            activity.startActivity(intent);
            return true;
        } else ActivityCompat.requestPermissions(activity, permissions, requestCode);

        return false;
    }

}
