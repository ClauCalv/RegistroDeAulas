package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.fragments.AnnotationsFragment;
import br.ufabc.gravador.views.widgets.DottedSeekBar;

public class OpenAudioActivity extends AbstractServiceActivity
        implements AnnotationsFragment.AnnotationFragmentListener {

    public final int play = android.R.drawable.ic_media_play, pause = android.R.drawable.ic_media_pause; //TODO hardcoded
    private int recordDuration, playTime;

    private ImageButton startStop, nextAnnotation, prevAnnotation;
    private TextView timeStamp, recordName;
    private DottedSeekBar progressBar;

    private Gravacao gravacao = null;
    private AnnotationsFragment fragment = null;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_open_audio, R.id.my_toolbar, true);

        startStop = findViewById(R.id.startStopPlaying);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                startStopOnClick(view);
            }
        });
        startStop.setImageResource(play);

        nextAnnotation = findViewById(R.id.nextAnnotation);
        nextAnnotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                nextPrevOnClick(view, true);
            }
        });

        prevAnnotation = findViewById(R.id.prevAnnotation);
        prevAnnotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                nextPrevOnClick(view, false);
            }
        });

        recordName = findViewById(R.id.recordName);
        recordName.setText("");

        timeStamp = findViewById(R.id.timeStamp);
        timeStamp.setText("0:00");

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

        fragment.updateGravacao();

        int serviceStatus = gravacaoService.getServiceStatus();

        startStop.setImageResource(serviceStatus == GravacaoService.STATUS_PLAYING ? play : pause);
        recordName.setText(gravacao.getName());
        timeStamp.setText(Gravacao.formatTime(0));
        progressBar.setDots(gravacao.getAnnotationTimes());
        progressBar.setMax(recordDuration);
    }

    @Override
    public void receiveFragment ( AnnotationsFragment f ) {
        fragment = f;
    }

    public int getGravacaoTime () { return !isBound ? 0 : (int) gravacaoService.getTime(); }

    @Override
    public Gravacao getGravacao () { return gravacao; }

    void nextPrevOnClick ( View view, boolean isNext ) {
        if ( !isBound ) return;
        playTime = gravacaoService.nextPrev(isNext);
        timeUpdate(playTime);
        fragment.jumpToTime(playTime);
    }

    void startStopOnClick ( View view ) {
        if ( !isBound ) return;
        if ( gravacaoService.getServiceStatus() == GravacaoService.STATUS_IDLE )
            gravacaoService.prepareGravacaoForPlaying();
        switch ( gravacaoService.getServiceStatus() ) {
            case GravacaoService.STATUS_PAUSED:
                if ( gravacaoService.startPausePlaying(true) ) {
                    startStop.setImageResource(pause);
                } else {
                    Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                            .show(); //TODO hardcoded
                }
            case GravacaoService.STATUS_PLAYING:
                if ( gravacaoService.startPausePlaying(false) ) {
                    startStop.setImageResource(play);
                } else {
                    Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                            .show(); //TODO hardcoded
                }
        }
    }

    public void timeUpdate ( int time ) {
        playTime = time;
        progressBar.setProgress(playTime);
        progressBar.setDots(gravacao.getAnnotationTimes());
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
    protected void onPause () {
        super.onPause();
        fragment.alertSave(new AnnotationsFragment.annotationSavedListener() {
            @Override
            public void onAnnotationSaved () {
                gravacao.saveGravacao(true, true);
            }
        });
        //TODO RELEASE
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
