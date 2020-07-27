package br.ufabc.gravador.views.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.widgets.DottedSeekBar;

public class OpenAudioActivity extends AbstractAnnotationsActivity {

    public final int play = R.drawable.ic_media_play, pause = R.drawable.ic_media_pause;
    private int recordDuration, playTime;

    private ImageButton startStop, nextAnnotation, prevAnnotation;
    private TextView timeStamp, recordName;
    private DottedSeekBar progressBar;

    private Gravacao gravacao = null;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_open_audio;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);

        startStop = findViewById(R.id.startStopPlaying);
        startStop.setOnClickListener(this::startStopOnClick);
        startStop.setImageResource(play);

        nextAnnotation = findViewById(R.id.nextAnnotation);
        nextAnnotation.setOnClickListener(( view ) -> nextPrevOnClick(view, true));

        prevAnnotation = findViewById(R.id.prevAnnotation);
        prevAnnotation.setOnClickListener(( view ) -> nextPrevOnClick(view, false));

        recordName = findViewById(R.id.recordName);
        recordName.setText("");

        timeStamp = findViewById(R.id.timeStamp);
        timeStamp.setText("00:00");

        progressBar = findViewById(R.id.progressBar);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged ( SeekBar seekBar, int i, boolean b ) {
                //TODO
            }

            @Override
            public void onStartTrackingTouch ( SeekBar seekBar ) {
            }

            @Override
            public void onStopTrackingTouch ( SeekBar seekBar ) {
                if ( !isBound ) return;
                gravacaoService.jumpTo(seekBar.getProgress());
                timeUpdate();
            }
        });

    }

    @Override
    protected void onServiceOnline () {
        if ( !gravacaoService.hasGravacao() ) {
            finish();
            Toast.makeText(null, "Falha ao abrir gravação", Toast.LENGTH_LONG).show();
            return;
        } else gravacao = gravacaoService.getGravacao();

        int serviceStatus = gravacaoService.getServiceStatus();

        startStop.setImageResource(serviceStatus == GravacaoService.STATUS_PLAYING ? pause : play);
        recordName.setText(gravacao.getName());
        timeStamp.setText(Gravacao.formatTime(0));
        progressBar.setDots(Arrays.stream(gravacao.getAnnotationTimes()).mapToInt(i -> i.time).toArray());
        progressBar.setMax(recordDuration = (int) gravacaoService.getTimeTotal());
        progressBar.invalidate();

        gravacaoService.setTimeUpdateListener(time -> {
            if (timeStamp != null)
                timeStamp.setText(Gravacao.formatTime(time));
            if (progressBar != null)
                progressBar.setProgress((int) time);
        });

        super.onServiceOnline();
    }

    void nextPrevOnClick ( View view, boolean isNext ) {
        if ( !isBound ) return;
        Gravacao.AnnotationTime playTime = gravacaoService.nextPrev(isNext);
        timeUpdate(playTime.time);
        annotationsFragment.openAnnotation(playTime.id);
    }

    void startStopOnClick ( View view ) {
        if ( !isBound ) return;
        if (gravacaoService.getServiceStatus() == GravacaoService.STATUS_IDLE) {
            gravacaoService.prepareForPlaying(GravacaoService.MEDIATYPE_AUDIO);
            gravacaoService.setupPlayer(null, null);
        }

        switch ( gravacaoService.getServiceStatus() ) {
            case GravacaoService.STATUS_PAUSED:
                gravacaoService.startPausePlaying(true);
                if (gravacaoService.getServiceStatus() == GravacaoService.STATUS_PLAYING) {
                    startStop.setImageResource(pause);
                } else {
                    Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                            .show(); //TODO hardcoded
                }
                break;
            case GravacaoService.STATUS_PLAYING:
                gravacaoService.startPausePlaying(false);
                if (gravacaoService.getServiceStatus() == GravacaoService.STATUS_PAUSED) {
                    startStop.setImageResource(play);
                } else {
                    Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                            .show(); //TODO hardcoded
                }
                break;
        }
    }

    public void timeUpdate ( int time ) {
        playTime = time;
        progressBar.setProgress(playTime);
        progressBar.setDots(Arrays.stream(gravacao.getAnnotationTimes()).mapToInt(i -> i.time).toArray());
        timeStamp.setText(Gravacao.formatTime(playTime));
    }

    public void timeUpdate () {
        timeUpdate(getGravacaoTime());
    }

    @Override
    public void onBackPressed () {
        if ( gravacao == null || !gravacao.isLastSaved() )
            new AlertDialog.Builder(this).setMessage(
                    "Descartar alterações? Não poderá desfazer esta ação")
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
    public void onAnnotationChanged ( int ID, boolean firsttime ) {
        if ( firsttime ) return;

        int time = gravacao.getAnnotation(ID).getTime();
        timeUpdate(gravacaoService.jumpTo(time));
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
    }
}
