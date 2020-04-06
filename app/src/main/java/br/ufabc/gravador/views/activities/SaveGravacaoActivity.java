package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import br.ufabc.gravador.R;
import br.ufabc.gravador.models.Gravacao;

public class SaveGravacaoActivity extends AbstractServiceActivity {

    private Button saveAll, saveRecord, saveAnnotations, saveNone;
    private int requestCode = 9510;
    private Gravacao gravacao;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_save_recordings, R.id.my_toolbar,
                true);

        saveAll = findViewById(R.id.saveAll);
        saveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                saveAllOnClick(view);
            }
        });

        saveRecord = findViewById(R.id.saveRecord);
        saveRecord.setOnClickListener(this::saveRecordOnClick);

        saveAnnotations = findViewById(R.id.saveAnnotation);
        saveAnnotations.setOnClickListener(this::saveAnnotationsOnClick);

        saveNone = findViewById(R.id.saveNone);
        saveNone.setOnClickListener(this::saveNoneOnClick);
    }

    @Override
    protected void onServiceOnline () {
        if ( !gravacaoService.hasGravacao() ) {
            finish();
            Toast.makeText(this, "Falha ao abrir gravação", Toast.LENGTH_LONG).show();
            return;
        } else gravacao = gravacaoService.getGravacao();
    }

    void saveAllOnClick ( View view ) {
        saveFile(true, true);
    }

    private void saveFile ( boolean record, boolean annotation ) {
        if ( !isBound ) return;

        gravacao.saveMode(record, annotation);
        Intent intent = new Intent(this, NameToSaveActivity.class);
        intent.putExtra("RequestCode", requestCode);
        startActivityForResult(intent, requestCode);
    }

    void saveRecordOnClick ( View view ) {

        if ( gravacao.hasAnnotation() )
            new AlertDialog.Builder(this).setMessage("Descartar anotações?")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            saveFile(true, false);
                        }
                    })
                    .setNegativeButton("Não", null)
                    .show();
        else saveFile(true, false);
    }

    void saveAnnotationsOnClick ( View view ) {

        new AlertDialog.Builder(this).setMessage("Descartar gravação?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick ( DialogInterface dialogInterface, int i ) {
                        saveFile(false, true);
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    void saveNoneOnClick ( View view ) {
        new AlertDialog.Builder(this).setMessage("Descartar tudo? Não poderá desfazer esta ação")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick ( DialogInterface dialogInterface, int i ) {
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, @Nullable Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode == this.requestCode )
            if ( resultCode == RESULT_OK ) {
                setResult(RESULT_OK);
                finish();
            }
    }

    @Override
    public void onBackPressed () {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onOptionsItemSelected ( MenuItem item ) {
        switch ( item.getItemId() ) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}