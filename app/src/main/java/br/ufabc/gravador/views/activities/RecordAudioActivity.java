package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.MyFileManager;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.fragments.AnnotationsFragment;

public class RecordAudioActivity extends AbstractServiceActivity
        implements AnnotationsFragment.AnnotationFragmentListener {

    public static int AUDIO_REQUEST = 1111, VIDEO_REQUEST = 2222; //    TODO video

    public final String start = "Iniciar Gravação", stop = "Terminar gravação", save = "Salvar Gravação"; //TODO hardcoded
    private Button startStop;
    private TextView finishedLabel, recordTimeText;
    private Gravacao gravacao = null;
    private AnnotationsFragment fragment = null;
    private MyFileManager fileManager;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_record_audio, R.id.my_toolbar, true);

        startStop = findViewById(R.id.startRecording);
        startStop.setOnClickListener(this::startStopOnClick);
        startStop.setText(start);

        finishedLabel = findViewById(R.id.finishedLabel);
        finishedLabel.setVisibility(View.INVISIBLE);

        recordTimeText = findViewById(R.id.recordTimeText);
        recordTimeText.setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onServiceOnline () {
        fileManager = MyFileManager.getInstance();
        fileManager.setup(getApplicationContext());

        if ( !gravacaoService.hasGravacao() ) {
            File f = fileManager.getDirectory(MyFileManager.GRAVACAO_DIR);
            gravacao = Gravacao.CreateEmpty(f.getPath(), MyFileManager.newTempName());
            gravacaoService.setGravacao(gravacao);
        } else gravacao = gravacaoService.getGravacao();
        fragment.updateGravacao();

        gravacaoService.setTimeUpdateListener(new GravacaoService.TimeUpdateListener() {
            @Override
            public void onTimeUpdate ( long time ) {
                if ( recordTimeText != null )
                    recordTimeText.setText(Gravacao.formatTime(time));
            }
        });
        updateState();
    }

    private void updateState () {
        if ( !isBound ) return;

        int serviceStatus = gravacaoService.getServiceStatus();

        startStop.setText(
                serviceStatus == GravacaoService.STATUS_RECORDING ? stop :
                        serviceStatus == GravacaoService.STATUS_WAITING_SAVE ? save :
                                start);
        finishedLabel.setVisibility(
                serviceStatus == GravacaoService.STATUS_WAITING_SAVE ? View.VISIBLE :
                        View.INVISIBLE);
        recordTimeText.setVisibility(
                serviceStatus == GravacaoService.STATUS_WAITING_SAVE ||
                        serviceStatus == GravacaoService.STATUS_RECORDING ? View.VISIBLE :
                        View.INVISIBLE);
    }

    @Override
    public void receiveFragment ( AnnotationsFragment f ) {
        fragment = f;
    }

    @Override
    public Gravacao getGravacao () { return gravacao; }

    @Override
    public int getGravacaoTime () {
        return isBound ? (int) gravacaoService.getTime() : 0;
    }

    void startStopOnClick ( View view ) {
        if ( !isBound ) return;

        if ( gravacaoService.getServiceStatus() == GravacaoService.STATUS_IDLE )
            gravacaoService.prepareForRecord(GravacaoService.MEDIATYPE_AUDIO);
        switch ( gravacaoService.getServiceStatus() ) {
            case GravacaoService.STATUS_RECORD_PREPARED:
                if ( !gravacaoService.startRecording() ) {
                    Toast.makeText(this, "Falha em iniciar gravação",
                            Toast.LENGTH_LONG).show(); //TODO hardcoded
                    gravacaoService.goIdle();
                }
                break;
            case GravacaoService.STATUS_RECORDING:
                gravacaoService.stopRecording();
                break;
            case GravacaoService.STATUS_WAITING_SAVE:
                fragment.alertSave(this::onAnnotationSaved);
                break;
        }

        updateState();
    }

    public void onAnnotationSaved () {
        Intent intent = new Intent(RecordAudioActivity.this,
                SaveGravacaoActivity.class);
        intent.putExtra("RequestCode", AUDIO_REQUEST);
        startActivityForResult(intent, AUDIO_REQUEST);
    }

    @Override
    public void onBackPressed () {
        if ( gravacao == null || !gravacao.isLastSaved() )
            new AlertDialog.Builder(this).setMessage(
                    "Descartar tudo? Não poderá desfazer esta ação")
                    .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            finish();
                        }
                    })
                    .setNegativeButton("Não", null)
                    .show();
        else finish();
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

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, @Nullable Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode == AUDIO_REQUEST )
            if ( resultCode == RESULT_OK ) {
                setResult(RESULT_OK);
                finish();
            }
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        gravacao.abortIfFailed();
    }

    @Override
    public void onAnnotationChanged ( int ID, boolean f ) { }

}
