package br.ufabc.gravador.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import br.ufabc.gravador.R;

public class JoinRoomActivity extends AbstractServiceActivity {

    Button readQRCode, joinRoomConfirm;
    TextView roomName;

    @SuppressWarnings( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_join_room, R.id.my_toolbar, true);

        readQRCode = findViewById(R.id.readQRCode);
        joinRoomConfirm = findViewById(R.id.joinRoomConfirm);
        roomName = findViewById(R.id.RoomName);

        readQRCode.setOnClickListener(this::readQRCodeOnClick);

        joinRoomConfirm.setOnClickListener(this::joinRoomConfirmOnClick);
    }

    @Override
    protected void onCreate ( Bundle savedInstanceState, int LayoutID, int ToolbarID, boolean homeEnabled ) {

    }

    @Override
    protected void onServiceOnline () {

    }

    void readQRCodeOnClick ( View view ) {}

    void joinRoomConfirmOnClick ( View view ) {}
}
