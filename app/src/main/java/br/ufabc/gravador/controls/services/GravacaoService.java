package br.ufabc.gravador.controls.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.ufabc.gravador.controls.helpers.ConnectionHelper;
import br.ufabc.gravador.controls.helpers.DirectoryHelper;
import br.ufabc.gravador.controls.helpers.NotificationHelper;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.models.GravacaoManager;

public class GravacaoService extends Service {

    public static final String SERVICE_ACTION = "SERVICE_ACTION";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE", ACTION_NEXT = "ACTION_NEXT", ACTION_PREV = "ACTION_PREV";
    public static final int MEDIATYPE_AUDIO = 1, MEDIATYPE_VIDEO = 2, MEDIATYPE_NULL = 0;
    public static final String AUDIO_EXTENSION = ".3gp", VIDEO_EXTENSION = ".mp4";
    public static final int STATUS_IDLE = -1, STATUS_RECORD_PREPARED = 0, STATUS_RECORDING = 1,
            STATUS_WAITING_SAVE = 2, STATUS_LOADING_SAVING = 3, STATUS_PLAYING = 4,
            STATUS_PAUSED = 5, STATUS_RECEIVING_TRANSMISSION = 6, STATUS_TRANSMITTING = 7;
    private static final int NOTIFICATION_ID = 44744;
    private final IBinder binder = new LocalBinder();

    private int currMediaType = MEDIATYPE_NULL;
    private int serviceStatus = STATUS_IDLE;
    private Camera camera;

    private NotificationHelper notificationHelper;
    private ConnectionHelper connectionHelper;
    private Gravacao gravacao;
    private GravacaoManager gh = null;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private DirectoryHelper dh;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private AudioFocusRequest focusRequest;
    private boolean shouldCameraRelease = false;
    private boolean useAudio = true;
    private Surface surface;

    public interface TimeUpdateListener {
        void onTimeUpdate ( long time );
    }
    private TimeUpdateListener registeredTimeListener;
    private long currentTime = 0, runnableStartTime = 0, timetotal = 0;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run () {
            currentTime = SystemClock.uptimeMillis() - runnableStartTime;
            timeHandler.postDelayed(this, timetotal > 20000 ? 500 : 200);
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

    public long getTime () { return currentTime; }

    public long getTimeTotal () { return timetotal; }

    public void setTimeTotal ( long timetotal ) { this.timetotal = timetotal; }

    @Override
    public void onCreate () {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        dh = new DirectoryHelper(this);
        if (gh == null) gh = new GravacaoManager(this, dh);
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
        stopPlaying();
        stopRecording();
        stopTimer();
    }

    public int getServiceStatus () {
        return serviceStatus;
    }

    public void goIdle () {
        serviceStatus = STATUS_IDLE;
        goBackground();
    }

    public void setGravacao ( Gravacao gravacao ) {
        this.gravacao = gravacao;
    }
    public boolean hasGravacao () {
        return gravacao != null;
    }
    public Gravacao getGravacao () { return gravacao; }

    public Gravacao createNewGravacao () {
        gravacao = gh.CreateEmpty(DirectoryHelper.newTempName());
        return gravacao;
    }

    public void setVideoParameters(Camera camera, boolean audio) {
        this.camera = camera;
        this.useAudio = audio;
    }

    public void scheduleCameraRelease() {
        if (camera == null) return;
        if (serviceStatus == STATUS_RECORDING) {
            camera.release();
            camera = null;
        } else shouldCameraRelease = true;
    }

    public void prepareForRecord ( int mediaType ) {
        if ( gravacao == null ) return;

        String location = null, extension = null;
        switch ( currMediaType = mediaType ) {
            case MEDIATYPE_AUDIO:
                location = dh.getDirectory(DirectoryHelper.AUDIO_DIR).getPath();
                extension = AUDIO_EXTENSION;
                break;
            case MEDIATYPE_VIDEO:
                location = dh.getDirectory(DirectoryHelper.VIDEO_DIR).getPath();
                extension = VIDEO_EXTENSION;
                break;
            case MEDIATYPE_NULL:
                return;
            default:
                throw new IllegalArgumentException("Unexpected mediatype: " + mediaType);
        }

        String tempname = DirectoryHelper.newTempName();
        File f = new File(location, tempname + extension);
        gravacao.setRecordURI(dh.createURIFromString(f.toString()).toString());

        serviceStatus = STATUS_RECORD_PREPARED;
    }

    public boolean startRecording () {
        if (
                getServiceStatus() != STATUS_RECORD_PREPARED ||
                        gravacao == null
        ) return false;

        try {
            recorder = new MediaRecorder();
            switch (currMediaType) {
                case MEDIATYPE_AUDIO:
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    break;
                case MEDIATYPE_VIDEO:
                    CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                    if (camera == null) throw new IllegalStateException("Camera not set");
                    recorder.setCamera(camera);
                    if (useAudio)
                        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                    recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    recorder.setOutputFormat(profile.fileFormat);
                    recorder.setVideoEncoder(profile.videoCodec);
                    recorder.setVideoEncodingBitRate(profile.videoBitRate);
                    recorder.setVideoFrameRate(profile.videoFrameRate);
                    recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                    if (useAudio) {
                        recorder.setAudioEncoder(profile.audioCodec);
                        recorder.setAudioChannels(profile.audioChannels);
                        recorder.setAudioEncodingBitRate(profile.audioBitRate);
                        recorder.setAudioSamplingRate(profile.audioSampleRate);
                    }
                    break;
                case MEDIATYPE_NULL:
                default:
                    return false;
            }
            ParcelFileDescriptor fd = dh.openFdFromString(gravacao.getRecordURI());
            recorder.setOutputFile(fd.getFileDescriptor());
            recorder.setOnErrorListener((MediaRecorder mr, int what, int extra) ->
                    Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra));
            recorder.prepare();
        } catch (IllegalStateException | IOException e) {
            Log.e("startRecord", "prepare() failed", e);
            Toast.makeText(this, "Falha em iniciar gravação", Toast.LENGTH_LONG).show();
            prepareForRecord(MEDIATYPE_NULL);
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
        if (recorder == null) return;

        recorder.stop();
        recorder.release();
        recorder = null;

        onRecordStopped();

        if (shouldCameraRelease) {
            shouldCameraRelease = false;
            scheduleCameraRelease();
        }
    }

    private void onRecordStopped () {
        stopTimer();
        buildSaveNotification();
        serviceStatus = STATUS_WAITING_SAVE;
    }

    public void saveGravacao(GravacaoManager.SaveListener l) {
        serviceStatus = STATUS_LOADING_SAVING;
        gh.setSaveListener(
                ( success ) -> {
                    onGravacaoSaved(success);
                    if ( l != null ) l.onGravacaoSaved(success);
                });
        gh.saveGravacao(gravacao);
    }

    public void onGravacaoSaved ( boolean success ) {
        if ( success ) goIdle();
        else serviceStatus = STATUS_WAITING_SAVE;
    }

    public void renameAndSave(String name, GravacaoManager.SaveListener l) {
        serviceStatus = STATUS_LOADING_SAVING;
        gh.setSaveListener(
                ( success ) -> {
                    onGravacaoSaved(success);
                    if ( l != null ) l.onGravacaoSaved(success);
                });
        gh.renameAndSave(gravacao, name);
    }

    public void removeRecordAndSave () {
        gh.removeRecordAndSave(gravacao);
    }

    private boolean isPlayerPrepared = false;

    public void setVideoSurface(Surface surface) {
        this.surface = surface;
    }

    public boolean prepareForPlaying(int mediaType) {
        if ( player == null )
            player = new MediaPlayer();
        else
            player.reset();

        try {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            player.setAudioAttributes(audioAttributes);
            player.setOnErrorListener(( MediaPlayer mp, int what, int extra ) -> {
                Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra);
                return false;
            });
            ParcelFileDescriptor fd = dh.openFdFromString(gravacao.getRecordURI());
            player.setDataSource(fd.getFileDescriptor());
            if ((currMediaType = mediaType) == MEDIATYPE_VIDEO)
                player.setSurface(surface);
            player.setLooping(false);
            player.prepare();
            isPlayerPrepared = true;
            currentTime = 0;
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    startPausePlaying(false);
                }
            });
        } catch ( IllegalArgumentException | IOException e ) {
            e.printStackTrace();
            Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG).show();
            stopPlaying();
            return false;
        }

        serviceStatus = STATUS_PAUSED;

        return true;
    }

    public boolean startPausePlaying ( boolean playPause ) {
        if ( player == null || !isPlayerPrepared )
            throw new IllegalStateException("player not prepared");

        try {
            if ( playPause ) {
                if (Build.VERSION.SDK_INT >= 26) {
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(audioAttributes)
                            .setAcceptsDelayedFocusGain(false)
                            .setWillPauseWhenDucked(false)
                            .build();
                    audioManager.requestAudioFocus(focusRequest);
                } else
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                long time = currentTime;
                player.start();
                startTimer(time);
                goForeground();
            } else {
                player.pause();
                if (Build.VERSION.SDK_INT >= 26)
                    audioManager.abandonAudioFocusRequest(focusRequest);
                else audioManager.abandonAudioFocus(null);
                stopTimer();
            }

            buildPlayPauseNotification(playPause);
            serviceStatus = playPause ? STATUS_PLAYING : STATUS_PAUSED;

            if ( playPause )
                if ( !player.isPlaying() )
                    throw new IllegalStateException("Failed silently");

        } catch ( IllegalStateException e ) {
            Log.e("MediaPlayer", "bad state", e);
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

    private boolean isJoiningPrepared = false;

    //     public boolean prepareForJoining () {
    //         connectionHelper = new ConnectionHelper(this);
    //
    //         connectionHelper.startDiscovering();
    //         connectionHelper.setConnectionListener(new ConnectionHelper.EmptyConnectionListener() {
    //             public void onDiscoveryFailed () { onFail(); }
    //             public void onEndpointDiscovered ( ConnectionHelper.Endpoint endpoint ) {
    //                 if(blablabla) connectionHelper.connectToEndpoint(endpoint,"teste");
    //             }
    //             public void onConnectionInitiated ( ConnectionHelper.Endpoint endpoint, ConnectionInfo connectionInfo ) {
    //                 if(blablabla) connectionHelper.acceptConnection(endpoint);
    //             }
    //             public void onConnectionFailed ( ConnectionHelper.Endpoint endpoint ) { onFail(); }
    //             public void onEndpointConnected ( ConnectionHelper.Endpoint endpoint ) {
    //
    //             }
    //             public void onEndpointDisconnected ( ConnectionHelper.Endpoint endpoint ) {
    //
    //             }
    //         });
    //
    //        serviceStatus = STATUS_RECEIVING_TRANSMISSION;
    //
    //        return true;
    //     }


    private void buildPlayPauseNotification ( boolean isPlaying ) {
        PendingIntent intent = null;
        switch (currMediaType) {
            case MEDIATYPE_AUDIO:
                intent = notificationHelper.buildPlayAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = notificationHelper.buildPlayVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newPlayNotification(
                        intent,
                        gravacao.getName(),
                        isPlaying
                )
        );
    }

    private void buildRecordNotification() {
        PendingIntent intent = null;
        switch (currMediaType) {
            case MEDIATYPE_AUDIO:
                intent = notificationHelper.buildRecordAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = notificationHelper.buildRecordVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newRecordNotification(intent)
        );
    }

    private void buildSaveNotification () {
        PendingIntent intent = null;
        switch (currMediaType) {
            case MEDIATYPE_AUDIO:
                intent = notificationHelper.buildRecordAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = notificationHelper.buildRecordVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        notificationHelper.pushNotification(
                NOTIFICATION_ID,
                notificationHelper.newSaveNotification(intent)
        );
    }

    private void goBackground () {
        stopForeground(true);
    }

    private void goForeground () {
        startForeground(NOTIFICATION_ID, notificationHelper.newBlankNotification());
    }
}
