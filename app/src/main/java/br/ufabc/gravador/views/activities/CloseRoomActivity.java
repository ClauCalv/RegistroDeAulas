package br.ufabc.gravador.views.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import br.ufabc.gravador.R;

//TODO extend abstractMenuActivity
public class CloseRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_room);
    }

}
