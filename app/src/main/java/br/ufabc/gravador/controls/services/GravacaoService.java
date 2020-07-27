package br.ufabc.gravador.controls.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.ufabc.gravador.controls.helpers.Camera2Helper;
import br.ufabc.gravador.controls.helpers.ConnectionHelper;
import br.ufabc.gravador.controls.helpers.DirectoryHelper;
import br.ufabc.gravador.controls.helpers.MediaHelper;
import br.ufabc.gravador.controls.helpers.NotificationHelper;
import br.ufabc.gravador.controls.helpers.TimerHelper;
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

    private NotificationHelper nh;
    private ConnectionHelper cnh;
    private Camera2Helper c2h;
    private DirectoryHelper dh;
    private TimerHelper th;
    private MediaHelper mh;

    private HandlerThread bkgThread;
    private Handler bkgHandler;

    private Gravacao gravacao;
    private GravacaoManager gh;
    private boolean useAudio = true;
    private Surface currPreviewSurface = null;

    public class LocalBinder extends Binder {
        public GravacaoService getService() {
            return GravacaoService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Camera2Helper.CameraDimensions cameraDim;

    @Override
    public void onCreate () {
        super.onCreate();

        bkgThread = new HandlerThread("bkg");
        bkgThread.start();
        bkgHandler = new Handler(bkgThread.getLooper());

        nh = new NotificationHelper(this);
        dh = new DirectoryHelper(this);
        gh = new GravacaoManager(this, dh);
        cnh = new ConnectionHelper(this);
        c2h = new Camera2Helper(this, bkgHandler);
        th = new TimerHelper(this);
        mh = new MediaHelper(this);
    }

    public int getServiceStatus() {
        return serviceStatus;
    }

    public void goIdle() {
        serviceStatus = STATUS_IDLE;
        goBackground();
    }

    public void setGravacao(Gravacao gravacao) {
        this.gravacao = gravacao;
    }

    public boolean hasGravacao() {
        return gravacao != null;
    }

    public Gravacao getGravacao() {
        return gravacao;
    }

    public Gravacao createNewGravacao() {
        gravacao = gh.CreateEmpty(DirectoryHelper.newTempName());
        return gravacao;
    }

    public long getTime() {
        return th.getTime();
    }

    public long getTimeTotal() {
        return th.getTimeTotal();
    }

    public void setTimeTotal(long total) {
        th.setTimeTotal(total);
    }

    public void setTimeUpdateListener(TimerHelper.TimeUpdateListener l) {
        th.setTimeUpdateListener(l);
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
                                startPausePlaying(!mh.isPlaying());
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
        th.stopTimer();
    }

    public void prepareGravacaoForRecord(int mediaType) {
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

    public void setupCamera(boolean facingFront, boolean requireSnapshots, Camera2Helper.CameraReadyCallback callback) {
        cameraDim = c2h.setupCamera(facingFront, requireSnapshots, callback);
    }

    public void registerPreviewSurface(Surface previewSurface) {
        currPreviewSurface = previewSurface;
        c2h.changePreviewSurface(previewSurface);
    }

    public void unregisterPreviewSurface() {
        registerPreviewSurface(null);
    }

    public void startPreviewing() {
        c2h.startPreviewing(currPreviewSurface);
    }

    public void stopPreviewing() {
        c2h.stopCaptureSession();
    }

    public boolean isAudioOn() {
        return useAudio;
    }

    public boolean toggleAudioOnOff() {
        useAudio = !useAudio;
        if (serviceStatus == STATUS_RECORDING) mh.setMicMuted(!useAudio);
        return useAudio;
    }

    public boolean startRecording () {
        if (getServiceStatus() != STATUS_RECORD_PREPARED || gravacao == null)
            return false;

        boolean isVideo = currMediaType == MEDIATYPE_VIDEO;

        ParcelFileDescriptor fd = dh.openFdFromString(gravacao.getRecordURI());
        Surface recorderSurface = mh.prepareRecorder(cameraDim, isVideo, fd.getFileDescriptor());
        mh.setMicMuted(!useAudio);
        if (isVideo) c2h.startRecording(currPreviewSurface, recorderSurface);
        mh.startRecording();

        onRecordStarted();
        return true;
    }

    private void onRecordStarted () {
        th.resetTimer();
        th.startTimer(0);
        goForeground();
        buildRecordNotification();
        serviceStatus = STATUS_RECORDING;
    }

    public void stopRecording () {
        mh.stopRecording();
        mh.setMicMuted(false);
        c2h.stopCaptureSession();
        onRecordStopped();
    }

    private void onRecordStopped () {
        th.stopTimer();
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

    public boolean prepareForPlaying(int mediaType) {
        ParcelFileDescriptor fd = dh.openFdFromString(gravacao.getRecordURI());
        mh.prepareForPlaying(mediaType == MEDIATYPE_VIDEO, fd.getFileDescriptor());
        th.resetTimer();
        serviceStatus = STATUS_PAUSED;

        return true;
    }

    public void setupPlayer(Surface output, MediaPlayer.OnCompletionListener onCompletionListener) {
        mh.setupPlayer(output, (MediaPlayer mp) -> {
            serviceStatus = STATUS_PAUSED;
            if (onCompletionListener != null) onCompletionListener.onCompletion(mp);
        });
    }

    public void startPausePlaying(boolean playPause) {
        if (playPause) {
            int curr = mh.startPlaying();
            th.startTimer(curr);
            goForeground();
        } else {
            mh.pausePlaying();
            th.stopTimer();
        }

        buildPlayPauseNotification(playPause);
        serviceStatus = playPause ? STATUS_PLAYING : STATUS_PAUSED;
    }

    public int jumpTo ( int time ) {
        th.stopTimer();
        mh.playerSeekTo(time);
        th.startTimer(time);
        return time;
    }

    public Gravacao.AnnotationTime nextPrev(boolean isNext) {

        Gravacao.AnnotationTime time;
        long currTime = th.getTime();
        Gravacao.AnnotationTime[] times = gravacao.getAnnotationTimes();

        List<Gravacao.AnnotationTime> lTimes = Arrays.stream(times)
                .filter(x -> isNext ?
                        x.time > currTime :
                        x.time < currTime)
                .sorted()
                .collect(Collectors.toList());

        time = lTimes.isEmpty() ?
                new Gravacao.AnnotationTime(-1, isNext ? mh.getDuration() : 0) :
                isNext ? lTimes.get(0) : lTimes.get(lTimes.size() - 1);

        jumpTo(time.time);
        return time;
    }

    private void stopPlaying () {
        mh.stopPlaying();
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
                intent = nh.buildPlayAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = nh.buildPlayVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        nh.pushNotification(
                NOTIFICATION_ID,
                nh.newPlayNotification(
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
                intent = nh.buildRecordAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = nh.buildRecordVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        nh.pushNotification(
                NOTIFICATION_ID,
                nh.newRecordNotification(intent)
        );
    }

    private void buildSaveNotification () {
        PendingIntent intent = null;
        switch (currMediaType) {
            case MEDIATYPE_AUDIO:
                intent = nh.buildRecordAudioPendingIntent();
            case MEDIATYPE_VIDEO:
                intent = nh.buildRecordVideoPendingIntent();
                break;
            case MEDIATYPE_NULL:
            default:
        }
        nh.pushNotification(
                NOTIFICATION_ID,
                nh.newSaveNotification(intent)
        );
    }

    private void goBackground () {
        stopForeground(true);
    }

    private void goForeground () {
        startForeground(NOTIFICATION_ID, nh.newBlankNotification());
    }

}
