package br.ufabc.gravador.views.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.DirectoryHelper;
import br.ufabc.gravador.models.Gravacao;

public class OpenGravacaoActivity extends AbstractServiceActivity {

    public final String REMOVE = "Remover gravacão do registro", ANNEX = "Anexar gravação ao registro"; //TODO hardcoded
    private Button changeRecord, playRecord, deleteGravacao;

    private Gravacao gravacao;
    private boolean hasRecord = false, hasAnnotation = false;
    private DirectoryHelper dh;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_open_gravacao;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);

        changeRecord = findViewById(R.id.changeRecord);
        changeRecord.setOnClickListener(this::changeRecordOnClick);
        changeRecord.setText(REMOVE);

        playRecord = findViewById(R.id.playRecord);
        playRecord.setOnClickListener(this::playRecordOnClick);
        playRecord.setEnabled(false);

        deleteGravacao = findViewById(R.id.deleteGravacao);
        deleteGravacao.setOnClickListener(this::deleteGravacaoOnClick);
        dh = new DirectoryHelper(this);
    }

    @Override
    protected void onServiceOnline () {
        if ( !gravacaoService.hasGravacao() ) {
            finish();
            Toast.makeText(this, "Erro em abrir gravação", Toast.LENGTH_LONG).show();
            return;
        } else gravacao = gravacaoService.getGravacao();
        updateRecordState();
    }

    public void updateRecordState () {
        if ( !isBound ) return;
        hasRecord = gravacao.hasRecord();
        hasAnnotation = gravacao.hasAnnotation();

        if ( changeRecord != null ) changeRecord.setText(hasRecord ? REMOVE : ANNEX);
        if ( playRecord != null ) playRecord.setEnabled(hasRecord || hasAnnotation);
    }

    public void removeRecord () {
        if ( !isBound ) return;
        gravacaoService.removeRecordAndSave();
        updateRecordState();
    }

    public void deleteAndQuit () {
        if ( !isBound ) return;
        new DirectoryHelper(this).deleteFileFromURI(gravacao.getAnnotationURI());
        gravacaoService.setGravacao(null);
        finish();
    }

    void changeRecordOnClick ( View view ) {
        if ( !isBound ) return;

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
            Toast.makeText(null, "Não implementado ainda", Toast.LENGTH_LONG).show();
        }
    }

    void playRecordOnClick ( View view ) {
        if ( !isBound ) return;

        String mimetype = null, duration = ""; //TODO continuar aqui

        if ( hasRecord ) {
            try {
                //MediaScannerConnection.scanFile(this, new String[]{gravacao.getRecordPath()}, null,null);
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                ParcelFileDescriptor fd = dh.openFdFromString(gravacao.getRecordURI());
                mmr.setDataSource(fd.getFileDescriptor());
                mimetype = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                mmr.release();
            } catch ( IllegalArgumentException e ) {
                Log.e("OpenGravacaoActivity", "Erro: ", e);
            }
        }

        if ( mimetype != null ) {
            Intent intent = null;
            Toast.makeText(this, "MimeType: " + mimetype + ", Duration: " + duration,
                    Toast.LENGTH_LONG).show();
            if ( mimetype.startsWith("audio") )
                intent = new Intent(this, OpenAudioActivity.class);
            else if ( mimetype.startsWith("video") )
                intent = new Intent(this, OpenVideoActivity.class);
            else
                ;//TODO

            if ( intent != null ) {
                gravacaoService.setTimeTotal(Long.valueOf(duration));
                startActivity(intent);
//                gravacaoService.prepareForPlaying();
                return;
            }
        }

        Toast.makeText(this, "Problema em reproduzir gravação", Toast.LENGTH_LONG).show();

    }

    void deleteGravacaoOnClick ( View view ) {
        if ( !isBound ) return;

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
