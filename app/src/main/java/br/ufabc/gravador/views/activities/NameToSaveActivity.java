package br.ufabc.gravador.views.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

import br.ufabc.gravador.R;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.models.GravacaoManager;

public class NameToSaveActivity extends AbstractServiceActivity {

    private Button saveRecordName;
    private TextView recordName;
    private Gravacao gravacao;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_name_to_save;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);

        saveRecordName = findViewById(R.id.saveRecordName);
        saveRecordName.setOnClickListener(this::saveRecordNameOnClick);

        recordName = findViewById(R.id.recordName);

    }

    @Override
    protected void onServiceOnline () {
        if ( !gravacaoService.hasGravacao() ) {
            finish();
            Toast.makeText(this, "Falha ao abrir gravação", Toast.LENGTH_LONG).show();
            return;
        } else gravacao = gravacaoService.getGravacao();

        recordName.setText(gravacao.getName());
    }

    void saveRecordNameOnClick ( View view ) {
        if ( !isBound ) return;

        if ( !Pattern.matches("\\w|(\\w[- \\w]*\\w)", recordName.getText()) ) {
            Toast.makeText(this, "Nome vazio ou inválido", Toast.LENGTH_LONG).show();
            return;
        }

        gravacaoService.renameAndSave(recordName.getText().toString(),
                new GravacaoManager.SaveListener() {
                    @Override
                    public void onGravacaoSaved ( boolean success ) {
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
