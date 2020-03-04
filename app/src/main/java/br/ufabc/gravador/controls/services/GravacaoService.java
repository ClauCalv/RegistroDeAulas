package br.ufabc.gravador.controls.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.ufabc.gravador.controls.helpers.MyFileManager;
import br.ufabc.gravador.controls.helpers.NotificationHelper;
import br.ufabc.gravador.models.Gravacao;

public class GravacaoService extends Service {

    public static final String SERVICE_ACTION = "SERVICE_ACTION";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE", ACTION_NEXT = "ACTION_NEXT", ACTION_PREV = "ACTION_PREV";
    public static final int MEDIATYPE_AUDIO = 1, MEDIATYPE_VIDEO = 2, MEDIATYPE_NULL = 0;
    public static final String AUDIO_EXTENSION = ".3gp", VIDEO_EXTENSION = "??";//TODO video
    public static final int STATUS_IDLE = 0, STATUS_RECORDING = 1, STATUS_WAITING_SAVE = 2, STATUS_LOADING_SAVING = 3, STATUS_PLAYING = 4, STATUS_PAUSED = 5;
    private static final int NOTIFICATION_ID = 44444;
    private final IBinder binder = new LocalBinder();

    private int currMediaType = MEDIATYPE_NULL;
    private int serviceStatus = STATUS_IDLE;

    private NotificationHelper notificationHelper;
    private Gravacao gravacao;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private MyFileManager fileManager;

    /*
     * TIME-LOGIC BLOCK
     */

    public interface TimeUpdateListener {
        void onTimeUpdate ( long time );
    }
    private TimeUpdateListener registeredTimeListener;
    private long currentTime = 0, runnableStartTime = 0;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run () {
            currentTime = SystemClock.uptimeMillis() - runnableStartTime;
            timeHandler.postDelayed(this, 500);
            onTimeUpdate();
        }
    };

    private void onTimeUpdate () {
        if ( registeredTimeListener != null ) registeredTimeListener.onTimeUpdate(currentTime);
    }

    public void setTimeUpdateListener ( TimeUpdateListener listener ) { registeredTimeListener = listener;}

    private void startTimer ( long offset ) {
        runnableStartTime = SystemClock.uptimeMillis() - offset;
        timeHandler.postDelayed(timeRunnable, 0);
    }

    private void stopTimer () { timeHandler.removeCallbacks(timeRunnable); }

    /*
     * END TIME-LOGIC
     */

    /*
     * LIFECYCLE-LOGIC BLOCK
     */

    @Override
    public void onCreate () {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        fileManager = MyFileManager.getInstance();
    }

    public class LocalBinder extends Binder {
        public GravacaoService getService () { return GravacaoService.this; }
    }

    @Override
    public IBinder onBind ( Intent intent ) {
        return binder;
    }

    @Override
    public int onStartCommand ( Intent intent, int flags, int startId ) {
        if ( intent != null )
            if ( intent.getExtras() != null ) {
                String action = intent.getExtras().getString(SERVICE_ACTION);
                if ( action != null ) // abriram uma notificação de play/pause
                    try {
                        switch ( action ) {
                            case ACTION_PLAY_PAUSE:
                                startPausePlaying(!player.isPlaying());
                                break;
                            case ACTION_NEXT:
                                nextPrev(true);
                                break;
                            case ACTION_PREV:
                                nextPrev(false);
                                break;
                        }
                    } catch ( IllegalStateException e ) {
                        Toast.makeText(null, "Erro na reprodução", Toast.LENGTH_LONG).show();
                    }
            }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        //TODO stopPlaying();
        stopRecording();
        stopTimer();
    }

    public int getServiceStatus () {
        return serviceStatus;
    }

    /*
     * END LIFECYCLE-LOGIC
     */

    /*
     *  RECORD-LOGIC BLOCK
     */

    public void setGravacao ( Gravacao gravacao ) {
        this.gravacao = gravacao;
    }

    public boolean hasGravacao () {
        return gravacao != null;
    }

    public void prepareGravacaoForRecord ( Gravacao g, int mediaType ) {
        if ( g == null ) return;
        String location = null, extension = null;
        switch ( mediaType ) {
            case MEDIATYPE_AUDIO:
                location = fileManager.getDirectory(MyFileManager.AUDIO_DIR).getPath();
                extension = AUDIO_EXTENSION;
                break;
            case MEDIATYPE_VIDEO:
                //TODO;
                break;
            case MEDIATYPE_NULL:
                break;
            default:
                throw new IllegalArgumentException("Unexpected mediatype: " + mediaType);
        }

        g.setFileLocation(location);
        g.setFileName(MyFileManager.newTempName());
        g.setFileExtension(extension);
    }

    private boolean startRecording () {
        if ( gravacao == null ) return false;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(gravacao.getFilePath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError ( MediaRecorder mr, int what, int extra ) {
                Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra);
            }
        });

        try {
            recorder.prepare();
        } catch ( IOException e ) {
            Log.e("AudioRecord", "prepare() failed", e);
            Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG);
            prepareGravacaoForRecord(gravacao, MEDIATYPE_NULL);
            return false;
        }

        recorder.start();
        onRecordStarted();
        return true;
    }

    private void onRecordStarted () {
        startTimer(0);
        goForeground();
        buildRecordNotification();
        serviceStatus = STATUS_RECORDING;
    }

    public void stopRecording () {
        if ( recorder != null ) {
            recorder.stop();
            recorder.release();
            recorder = null;
            onRecordStopped();
        }
    }

    private void onRecordStopped () {
        stopTimer();
        buildSaveNotification();
        serviceStatus = STATUS_WAITING_SAVE;
    }

    public void saveGravacaoAssync ( boolean record, boolean annotations ) {
        new Handler().post(new Runnable() {
            @Override
            public void run () {
                gravacao.saveGravacao(record, annotations);
            }
        });
    }

    /*
     * END RECORD-LOGIC
     */

    /*
     * PLAYER-LOGIC BLOCK
     */

    private boolean isPlayerPrepared = false;

    public boolean prepareGravacaoForPlaying ( Gravacao gravacao ) {
        if ( player == null )
            player = new MediaPlayer();
        else
            player.reset();

        try {
            player.setDataSource(gravacao.getFilePath());
            player.setLooping(false);
            player.prepare();
            isPlayerPrepared = true;
        } catch ( IllegalArgumentException | IOException e ) {
            e.printStackTrace();
            Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG).show();
            stopPlaying();
            return false;
        }
        return true;
    }

    public boolean startPausePlaying ( boolean playPause ) {
        if ( player == null || !isPlayerPrepared )
            throw new IllegalStateException("player not prepared");

        try {
            if ( playPause ) {
                long time = currentTime;
                player.start();
                startTimer(time);
                goForeground();
            } else {
                player.pause();
                stopTimer();
            }

            buildPlayPauseNotification(playPause);
            serviceStatus = playPause ? STATUS_PLAYING : STATUS_PAUSED;

        } catch ( IllegalStateException e ) {
            Log.e("AudioPlayer", "bad state", e);
            stopPlaying();
            return false;
        }
        return true;
    }

    public int jumpTo ( int time ) {
        if ( player == null || !isPlayerPrepared )
            throw new IllegalStateException("player not prepared");

        stopTimer();
        player.seekTo(time);
        startTimer(time);
        return time;
    }

    public int nextPrev ( boolean isNext ) {
        if ( player == null || !isPlayerPrepared )
            throw new IllegalStateException("player not prepared");

        int time;
        long currTime = currentTime;
        int[] times = gravacao.getAnnotationTimes();

        List<Integer> lTimes = Arrays.stream(times)
                .filter(x -> isNext ?
                        x > currTime :
                        x < currTime)
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        time = lTimes.isEmpty() ?
                isNext ? player.getDuration() : 0 :
                isNext ? lTimes.get(0) : lTimes.get(lTimes.size() - 1);

        return jumpTo(time);
    }

    private void stopPlaying () {
        if ( player != null ) {
            player.stop();
            player.release();
            player = null;
            isPlayerPrepared = false;
        }
        goBackground();
        serviceStatus = STATUS_IDLE;
    }

    /*
     * END PLAYER-LOGIC
     */

    /*
     * NOTIFICATION-LOGIC BLOCK
     */

    private void buildPlayPauseNotification ( boolean isPlaying ) {
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newPlayAudioNotification(
                        notificationHelper.buildPlayAudioPendingIntent(),
                        gravacao.getName(),
                        isPlaying
                )
        );
    }

    private void buildRecordNotification () {
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newRecordAudioNotification(
                        notificationHelper.buildRecordPendingIntent()
                )
        );
    }

    private void buildSaveNotification () {
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newSaveNotification(
                        notificationHelper.buildSavePendingIntent()
                ));
    }

    private void goBackground () {
        notificationHelper.clearNotifications();
    }

    private void goForeground () {
        startForeground(NOTIFICATION_ID, notificationHelper.newBlankNotification());
    }
}
