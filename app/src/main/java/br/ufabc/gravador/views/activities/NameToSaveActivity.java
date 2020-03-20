package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;

public class NameToSaveActivity extends AbstractServiceActivity {

    private Button saveRecordName;
    private TextView recordName;
    private Gravacao gravacao;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_name_to_save, R.id.my_toolbar, true);

        saveRecordName = findViewById(R.id.saveRecordName);
        saveRecordName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                saveRecordNameOnClick(view);
            }
        });

        recordName = findViewById(R.id.recordName);

    }

    @Override
    protected void onServiceOnline () {
        if ( !gravacaoService.hasGravacao() ) {
            finish();
            Toast.makeText(null, "Falha ao abrir gravação", Toast.LENGTH_LONG).show();
            return;
        } else gravacao = gravacaoService.getGravacao();

        recordName.setText(gravacao.getRecordName());
    }

    void saveRecordNameOnClick ( View view ) {
        if ( !isBound ) return;

        if ( !Pattern.matches("\\w|(\\w[- \\w]*\\w)", recordName.getText()) ) {
            Toast.makeText(this, "Nome vazio ou inválido", Toast.LENGTH_LONG).show();
            return;
        }

        gravacaoService.renameAndSaveAssync(recordName.getText().toString(),
                new GravacaoService.onGravacaoSavedListener() {
                    @Override
                    public void onSaved ( boolean success ) {
                        if ( success ) {
                            Toast.makeText(NameToSaveActivity.this,
                                    "Salvo com sucesso", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(NameToSaveActivity.this,
                                    "Falha em salvar. Nome já existente?", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                });
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
