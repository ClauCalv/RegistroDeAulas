package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.ufabc.gravador.R;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.fragments.AnnotationsFragment;
import br.ufabc.gravador.views.widgets.DottedSeekBar;

public class OpenAudioActivity extends AbstractMenuActivity
        implements AnnotationsFragment.AnnotationFragmentListener {

    public final int play = android.R.drawable.ic_media_play, pause = android.R.drawable.ic_media_pause; //TODO hardcoded
    private boolean isPlaying = false;
    private int recordDuration, playTime;

    private ImageButton startStop, nextAnnotation, prevAnnotation;
    private TextView timeStamp, recordName;
    private DottedSeekBar progressBar;

    private Gravacao gravacao = null;
    private AnnotationsFragment fragment = null;
    private AudioOpenerRetainedFragment audioFragment;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_open_audio, R.id.my_toolbar, true,
                AudioOpenerRetainedFragment.TAG);

        audioFragment = (AudioOpenerRetainedFragment) dataFragment;

        Bundle extras = getIntent().getExtras();
        if ( extras != null ) {
            gravacao = Gravacao.postedInstance;
            recordDuration = extras.getInt("Duration");
        }
        if ( savedInstanceState != null ) {
            isPlaying = savedInstanceState.getBoolean("isPlaying");
            Log.wtf("OpenAudio", "isPlaying = " + isPlaying);
        }

        startStop = findViewById(R.id.startStopPlaying);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                startStopOnClick(view);
            }
        });
        startStop.setImageResource(isPlaying ? pause : play);

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
        recordName.setText(gravacao.getName());

        timeStamp = findViewById(R.id.timeStamp);
        timeStamp.setText(Gravacao.formatTime(0));

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
                audioFragment.jumpTo(gravacao, seekBar.getProgress());
                timeUpdate();
            }
        });
        progressBar.setDots(gravacao.getAnnotationTimes());
        progressBar.setMax(recordDuration);

    }

    @Override
    protected RetainedFragment newRetainedFragment () {
        return new AudioOpenerRetainedFragment();
    }

    @Override
    public void receiveFragment ( AnnotationsFragment f ) {
        fragment = f;
    }

    public int getGravacaoTime () { return ( audioFragment.getGravacaoTime() ); }

    @Override
    public Gravacao getGravacao () {
        return gravacao;
    }


    void nextPrevOnClick ( View view, boolean isNext ) {
        playTime = audioFragment.nextPrev(gravacao, isNext);
        timeUpdate(playTime);
        fragment.jumpToTime(playTime);
    }

    void startStopOnClick ( View view ) {
        if ( !isPlaying ) {
            if ( audioFragment.startStopPlaying(gravacao, true) ) {
                audioFragment.startTimer();
                startStop.setImageResource(pause);
            } else {
                Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                        .show(); //TODO hardcoded
            }
        } else {
            if ( audioFragment.startStopPlaying(gravacao, false) ) {
                audioFragment.stopTimer();
                startStop.setImageResource(play);
            } else {
                Toast.makeText(this, "Falha em iniciar reprodução", Toast.LENGTH_LONG)
                        .show(); //TODO hardcoded
            }
        }
        isPlaying = !isPlaying;
    }

    public void timeUpdate ( int time ) {
        playTime = time;
        progressBar.setProgress(playTime);
        progressBar.setDots(gravacao.getAnnotationTimes());
        timeStamp.setText(Gravacao.formatTime(playTime));
    }

    public void timeUpdate () {
        timeUpdate(audioFragment.getGravacaoTime());
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
        timeUpdate(audioFragment.jumpTo(gravacao, time));
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState ( @NonNull Bundle outState ) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPlaying", isPlaying);
        Log.wtf("OpenAudio", "saving isPlaying as " + isPlaying);
    }

    public static class AudioOpenerRetainedFragment extends AbstractMenuActivity.RetainedFragment {

        public static String TAG = "AudioRecordRetainedFragment";

        // --- AUDIO RECORD ---
        private MediaPlayer mediaPlayer = null;
        // --- TIME ---
        private Handler timeHandler = new Handler();
        private int playTime = 0;
        private boolean isPrepared = false;
        // --- END AUDIO RECORD ---
        private Runnable timeRunnable = new Runnable() {
            @Override
            public void run () {
                if ( mediaPlayer != null ) playTime = mediaPlayer.getCurrentPosition();
                timeHandler.postDelayed(this, 500);
                Activity a = getActivity();
                if ( a instanceof OpenAudioActivity )
                    ( (OpenAudioActivity) a ).timeUpdate();
            }
        };

        private int getGravacaoTime () {
            return playTime;
        }

        private boolean configPlayer ( Gravacao gravacao ) {
            if ( mediaPlayer == null )
                mediaPlayer = new MediaPlayer();
            else
                mediaPlayer.reset();

            try {
                mediaPlayer.setDataSource(gravacao.getFilePath());
                mediaPlayer.setLooping(false);
                mediaPlayer.prepare();
                isPrepared = true;
            } catch ( IllegalArgumentException | IOException e ) {
                e.printStackTrace();
                Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG).show();
                stopPlaying();
                return false;
            }
            return true;
        }

        private boolean startStopPlaying ( Gravacao gravacao, boolean playPause ) {
            if ( mediaPlayer == null || !isPrepared )
                if ( !configPlayer(gravacao) ) return false;

            try {
                if ( playPause )
                    mediaPlayer.start();
                else
                    mediaPlayer.pause();

            } catch ( IllegalStateException e ) {
                Log.e("AudioPlayer", "bad state", e);
                stopPlaying();
                return false;
            }
            return true;
        }

        private int jumpTo ( Gravacao gravacao, int time ) {
            if ( mediaPlayer == null || !isPrepared )
                if ( !configPlayer(gravacao) )
                    return 0;

            mediaPlayer.seekTo(time);
            this.playTime = time;
            return time;
        }

        private int nextPrev ( Gravacao gravacao, boolean isNext ) {
            if ( mediaPlayer == null || !isPrepared )
                if ( !configPlayer(gravacao) ) return 0;

            int time, playTime = this.playTime;
            int[] times = gravacao.getAnnotationTimes();

            List<Integer> lTimes = Arrays.stream(times)
                    .filter(x -> isNext ? x > playTime : x < playTime)
                    .sorted()
                    .boxed()
                    .collect(Collectors.toList());

            time = lTimes.isEmpty() ?
                    isNext ? mediaPlayer.getDuration() : 0 :
                    isNext ? lTimes.get(0) : lTimes.get(lTimes.size() - 1);

            return jumpTo(gravacao, time);
        }

        private void stopPlaying () {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }

        void startTimer () {
            timeHandler.postDelayed(timeRunnable, 0);
        }

        void stopTimer () { timeHandler.removeCallbacks(timeRunnable); }

        // --- END TIME ---

        @Override
        public void onDestroy () {
            super.onDestroy();
            if ( mediaPlayer != null ) stopPlaying();
            if ( timeHandler != null ) stopTimer();
        }
    }

}
