package br.ufabc.gravador.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.PermissionHelper;

public class SharedRecordActivity extends AbstractMenuActivity {

    Button joinRoom, newRoom;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_shared_record;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {

        joinRoom = findViewById(R.id.joinRoom);
        newRoom = findViewById(R.id.newRoom);

        joinRoom.setOnClickListener(this::joinRoomOnClick);
        newRoom.setOnClickListener(this::newRoomOnClick);
    }

    void joinRoomOnClick ( View view ) {
        PermissionHelper ph = new PermissionHelper(this);
        Intent intent = new Intent(this, JoinRoomActivity.class);
        ph.startIfPermitted(intent, PermissionHelper.REQUEST_STREAM,
                PermissionHelper.STREAM_PERMISSIONS);
    }

    void newRoomOnClick ( View view ) {
        PermissionHelper ph = new PermissionHelper(this);
        Intent intent = new Intent(this, CreateRoomActivity.class);
        ph.startIfPermitted(intent, PermissionHelper.REQUEST_STREAM,
                PermissionHelper.STREAM_PERMISSIONS);
    }
}
