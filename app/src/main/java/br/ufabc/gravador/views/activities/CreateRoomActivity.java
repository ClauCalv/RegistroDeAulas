package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import br.ufabc.gravador.R;

public class CreateRoomActivity extends AbstractMenuActivity {

    private Button createRoom;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_create_room, R.id.my_toolbar, true);

        createRoom = findViewById(R.id.createRoom);
        createRoom.setOnClickListener(this::createRoomOnClick);
    }

    public void onResume () {
        super.onResume();
    }

    @Override
    public void onPause () {
        super.onPause();
    }

    public void createRoomOnClick ( View view ) {
    }
}