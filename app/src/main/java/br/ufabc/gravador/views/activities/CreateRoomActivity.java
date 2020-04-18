package br.ufabc.gravador.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import br.ufabc.gravador.R;

public class CreateRoomActivity extends AbstractServiceActivity {

    private Button createRoom;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_create_room;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);

        createRoom = findViewById(R.id.createRoom);
        createRoom.setOnClickListener(this::createRoomOnClick);
    }

    @Override
    protected void onServiceOnline() {

    }

    public void createRoomOnClick ( View view ) {
    }
}