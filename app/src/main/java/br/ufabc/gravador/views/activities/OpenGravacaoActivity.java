package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import br.ufabc.gravador.R;
import br.ufabc.gravador.models.Gravacao;

public class OpenGravacaoActivity extends AbstractMenuActivity {

    public final String REMOVE = "Remover gravacão do registro", ANNEX = "Anexar gravação ao registro"; //TODO hardcoded
    private Button changeRecord, playRecord, deleteGravacao;

    private Gravacao gravacao;
    private boolean hasRecord = false, hasAnnotation = false;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_open_gravacao, R.id.my_toolbar, true,
                null);

        gravacao = Gravacao.postedInstance;
        if ( gravacao == null ) {
            Toast.makeText(this, "Erro em abrir gravação", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        changeRecord = findViewById(R.id.changeRecord);
        changeRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                changeRecordOnClick(view);
            }
        });

        playRecord = findViewById(R.id.playRecord);
        playRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                playRecordOnClick(view);
            }
        });

        deleteGravacao = findViewById(R.id.deleteGravacao);
        deleteGravacao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                deleteGravacaoOnClick(view);
            }
        });

        updateRecordState();
    }

    public void updateRecordState () {
        if ( gravacao != null ) {
            hasRecord = gravacao.hasRecord();
            hasAnnotation = gravacao.hasAnnotation();
        }
        if ( changeRecord != null ) changeRecord.setText(hasRecord ? REMOVE : ANNEX);
        if ( playRecord != null ) playRecord.setEnabled(hasRecord || hasAnnotation);
    }

    public void removeRecord () {
        gravacao.removeRecord();
        updateRecordState();
    }

    public void deleteAndQuit () {
        File f = new File(gravacao.getAnnotationPath());
        f.delete();
        finish();
    }

    void changeRecordOnClick ( View view ) {
        if ( hasRecord ) {
            new AlertDialog.Builder(this)
                    .setMessage("Remover gravação? Não poderá desfazer esta ação")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            removeRecord();
                        }
                    }).setNegativeButton("Não", null).show();
        } else {
            //TODO findFileToAnnex
        }
    }

    void playRecordOnClick ( View view ) {
        String mimetype = null, duration = "";

        if ( hasRecord ) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(gravacao.getFilePath());
            mimetype = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            mmr.release();
        }

        if ( mimetype != null ) {
            Intent intent = null;
            Toast.makeText(this, "MimeType: " + mimetype + ", Duration: " + duration,
                    Toast.LENGTH_LONG).show();
            if ( mimetype.startsWith("audio") )
                intent = new Intent(this, OpenAudioActivity.class);
            else if ( mimetype.startsWith("video") )
                ;//TODO
            else
                ;//TODO

            if ( intent != null ) {
                intent.putExtra("Duration", Integer.valueOf(duration));
                startActivity(intent);
                return;
            }
        }

        Toast.makeText(this, "Problema em reproduzir gravação", Toast.LENGTH_LONG).show();

    }

    void deleteGravacaoOnClick ( View view ) {
        new AlertDialog.Builder(this).setMessage("Deletar registro? Não poderá desfazer esta ação")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick ( DialogInterface dialogInterface, int i ) {
                        deleteAndQuit();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }
}
