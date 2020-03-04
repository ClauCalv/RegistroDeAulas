package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.MyFileManager;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.fragments.AnnotationsFragment;

public class RecordAudioActivity extends AbstractMenuActivity
        implements AnnotationsFragment.AnnotationFragmentListener {

    public static int AUDIO_REQUEST = 1111, VIDEO_REQUEST = 2222; //    TODO video

    public final String start = "Iniciar Gravação", stop = "Terminar gravação", save = "Salvar Gravação"; //TODO hardcoded
    private Button startStop;
    private TextView finishedLabel, recordTimeText;
    //private Gravacao gravacao = null;
    private AnnotationsFragment fragment = null;
    private MyFileManager fileManager;
    private GravacaoService gravacaoService;
    private ServiceConnection serviceConnection;
    private boolean isBound = false;

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_record_audio, R.id.my_toolbar, true,
                null);

        Intent intent = new Intent(this, GravacaoService.class);
        startService(intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected ( ComponentName className, IBinder binder ) {
                gravacaoService = ( (GravacaoService.LocalBinder) binder ).getService();
                isBound = true;
            }

            @Override
            public void onServiceDisconnected ( ComponentName arg0 ) {
                isBound = false;
            }
        };
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        fileManager = MyFileManager.getInstance();
        fileManager.setup(getApplicationContext());

        if ( !gravacaoService.hasGravacao() ) {
            File f = fileManager.getDirectory(MyFileManager.GRAVACAO_DIR);
            gravacaoService.setGravacao(
                    Gravacao.CreateEmpty(f.getPath(), MyFileManager.newTempName()));
        }

        startStop = findViewById(R.id.startRecording);
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                startStopOnClick(view);
            }
        });

        int serviceStatus = gravacaoService.getServiceStatus();
        startStop.setText(
                serviceStatus == GravacaoService.STATUS_RECORDING ? stop :
                        serviceStatus == GravacaoService.STATUS_WAITING_SAVE ? save :
                                start);

        finishedLabel = findViewById(R.id.finishedLabel);
        finishedLabel.setVisibility(finished ? View.VISIBLE : View.INVISIBLE);

        recordTimeText = findViewById(R.id.recordTimeText);
        recordTimeText.setVisibility(recording ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected RetainedFragment newRetainedFragment () {
        return new AudioRecordRetainedFragment();
    }

    @Override
    public void receiveFragment ( AnnotationsFragment f ) {
        fragment = f;
    }

    public int getGravacaoTime () { return (int) audioFragment.recordTime; }

    @Override
    public Gravacao getGravacao () {
        return gravacao;
    }

    void startStopOnClick ( View view ) {
        if ( !recording ) {
            if ( audioFragment.startRecording(gravacao, fileManager) ) {
                audioFragment.startTimer();
                startStop.setText(stop);
                recording = !recording;
            } else {
                Toast.makeText(this, "Falha em iniciar gravação",
                        Toast.LENGTH_LONG).show(); //TODO hardcoded
            }
        } else if ( !finished ) {
            audioFragment.stopRecording();
            audioFragment.stopTimer();
            startStop.setText(save);
            finishedLabel.setVisibility(View.VISIBLE);
            finished = true;
            gravacao.sucess();
        } else {
            gravacao.post();
            fragment.alertSave(true);
        }
    }

    @Override
    public void alertSaveReturn () {
        Intent intent = new Intent(this, SaveGravacaoActivity.class);
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
    protected void onPause () {
        super.onPause();
        //TODO RELEASE
    }
    @Override
    protected void onDestroy () {
        super.onDestroy();
        gravacao.abortIfFailed();
    }

    @Override
    public void onSaveInstanceState ( @NonNull Bundle outState ) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("recording", recording);
        outState.putBoolean("finished", finished);
        gravacao.post();
    }

    @Override
    public void onAnnotationChanged ( int ID, boolean f ) { }

    //    public static class AudioRecordRetainedFragment extends AbstractMenuActivity.RetainedFragment {
    //
    //        public static String TAG = "AudioRecordRetainedFragment";
    //
    //        // --- AUDIO RECORD ---
    //        private MediaRecorder recorder = null;
    //        // --- TIME ---
    //        private Handler timeHandler = new Handler();
    //        private long recordTime = 0, startTime = 0;
    //        // --- END AUDIO RECORD ---
    //        private Runnable timeRunnable = new Runnable() {
    //            @Override
    //            public void run () {
    //                recordTime = SystemClock.uptimeMillis() - startTime;
    //                timeHandler.postDelayed(this, 500);
    //                Activity a = getActivity();
    //                if ( a != null ) {
    //                    TextView t = ( (RecordAudioActivity) a ).recordTimeText;
    //                    if ( t != null )
    //                        t.setText(Gravacao.formatTime(recordTime));
    //                }
    //            }
    //        };
    //
    //        private boolean startRecording ( Gravacao gravacao, MyFileManager fileManager ) {
    //            gravacao.setFileLocation(fileManager.getDirectory(MyFileManager.AUDIO_DIR).getPath());
    //            gravacao.setFileName(MyFileManager.newTempName());
    //            gravacao.setFileExtension(".3gp");
    //
    //            recorder = new MediaRecorder();
    //            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    //            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    //            recorder.setOutputFile(gravacao.getFilePath());
    //            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    //            recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
    //                @Override
    //                public void onError ( MediaRecorder mr, int what, int extra ) {
    //                    Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra);
    //                }
    //            });
    //
    //            try {
    //                recorder.prepare();
    //            } catch ( IOException e ) {
    //                Log.e("AudioRecord", "prepare() failed", e);
    //                Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG);
    //                gravacao.setFileLocation(null);
    //                gravacao.setFileName(null);
    //                gravacao.setFileExtension(null);
    //                return false;
    //            }
    //
    //            recorder.start();
    //            return true;
    //        }
    //
    //        private void stopRecording () {
    //            recorder.stop();
    //            recorder.release();
    //            recorder = null;
    //        }
    //
    //        void startTimer () {
    //            startTime = SystemClock.uptimeMillis();
    //            timeHandler.postDelayed(timeRunnable, 0);
    //            Activity a = getActivity();
    //            if ( a != null ) ( (RecordAudioActivity) a ).recordTimeText.setVisibility(View.VISIBLE);
    //        }
    //
    //        void stopTimer () { timeHandler.removeCallbacks(timeRunnable); }
    //
    //        // --- END TIME ---
    //
    //        @Override
    //        public void onDestroy () {
    //            super.onDestroy();
    //            if ( recorder != null ) stopRecording();
    //            if ( timeHandler != null ) stopTimer();
    //        }
    //    }

}
