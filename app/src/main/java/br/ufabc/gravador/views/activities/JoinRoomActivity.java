package br.ufabc.gravador.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import br.ufabc.gravador.R;

public class JoinRoomActivity extends AbstractServiceActivity {

    Button readQRCode, joinRoomConfirm;
    TextView roomName;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_join_room;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);

        readQRCode = findViewById(R.id.readQRCode);
        joinRoomConfirm = findViewById(R.id.joinRoomConfirm);
        roomName = findViewById(R.id.roomName);

        readQRCode.setOnClickListener(this::readQRCodeOnClick);

        joinRoomConfirm.setOnClickListener(this::joinRoomConfirmOnClick);
    }

    @Override
    protected void onServiceOnline () {

    }

    void readQRCodeOnClick ( View view ) {}

    void joinRoomConfirmOnClick ( View view ) {}
}
