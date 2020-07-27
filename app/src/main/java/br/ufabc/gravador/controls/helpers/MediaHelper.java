package br.ufabc.gravador.controls.helpers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;

public class MediaHelper {

    private final Context context;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private AudioFocusRequest focusRequest;
    private boolean isMediaRecorderPrepared = false;
    private boolean isMediaPlayerPrepared = false;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    public MediaHelper(Context context) {
        this.context = context;
    }

    public Surface prepareRecorder(Camera2Helper.CameraDimensions dims, boolean isVideo, FileDescriptor fd) {
        try {
            recorder = new MediaRecorder();
            if (isVideo) {
                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setVideoEncodingBitRate(1024 * 1024);
                recorder.setVideoFrameRate(30);
                recorder.setVideoSize(dims.vd.getWidth(), dims.vd.getHeight());
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setOrientationHint(dims.d);
            } else {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            }

            recorder.setOutputFile(fd);
            recorder.setOnErrorListener((MediaRecorder mr, int what, int extra) ->
                    Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra));
            recorder.prepare();

        } catch (IllegalStateException | IOException e) {
            Log.e("prepareRecorder", "prepare() failed", e);
            recorder.reset();
            return null;
        }

        isMediaRecorderPrepared = true;

        if (isVideo) return recorder.getSurface();
        return null;
    }

    public void startRecording() {
        if (!isMediaRecorderPrepared) throw new IllegalStateException("recorder not prepared");
        recorder.start();
        isRecording = true;
    }

    public void stopRecording() {
        try {
            recorder.stop();
        } finally {
            recorder.reset();
            isMediaRecorderPrepared = false;
        }
    }

    public boolean prepareForPlaying(boolean isVideo, FileDescriptor fd) {
        try {
            if (player == null) player = new MediaPlayer();
            else player.reset();

            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            player.setAudioAttributes(audioAttributes);
            player.setOnErrorListener((MediaPlayer mp, int what, int extra) -> {
                Log.e("MediaRecorder ERROR", "what = " + what + ", extra = " + extra);
                return false;
            });
            player.setDataSource(fd);
            player.setLooping(false);
            player.prepare();
            isMediaPlayerPrepared = true;
        } catch (IllegalArgumentException | IOException e) {
            e.printStackTrace();
            Toast.makeText(null, "Falha em iniciar gravação", Toast.LENGTH_LONG).show();
            player.reset();
            return false;
        }

        return true;
    }

    public void setupPlayer(Surface output, MediaPlayer.OnCompletionListener listener) {
        player.setSurface(output);
        player.setOnCompletionListener(listener);
    }

    public int startPlaying() {
        if (!isMediaPlayerPrepared) throw new IllegalStateException("player not prepared");
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(false)
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            } else
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            player.start();

            isPlaying = true;
        } catch (IllegalStateException e) {
            Log.e("MediaPlayer", "bad state", e);
            player.reset();
            return -1;
        }
        return player.getCurrentPosition();
    }

    public void pausePlaying() {
        try {
            player.pause();

            if (Build.VERSION.SDK_INT >= 26)
                audioManager.abandonAudioFocusRequest(focusRequest);
            else audioManager.abandonAudioFocus(null);

            isPlaying = false;
        } catch (IllegalStateException e) {
            Log.e("MediaPlayer", "bad state", e);
            player.reset();
        }
    }

    public int getDuration() {
        try {
            return player.getDuration();
        } catch (Exception e) { //for whatever reason including nullity or bad file
            return 0;
        }
    }

    public void playerSeekTo(int time) {
        player.seekTo(time);
    }

    public void stopPlaying() {
        try {
            player.stop();
        } finally {
            player.reset();
            isMediaPlayerPrepared = false;
        }
    }

    public boolean isPlaying() {
        if (player == null) return false;
        return player.isPlaying();
    }

    public void setMicMuted(boolean state) {
        AudioManager myAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // get the working mode and keep it
        int workingAudioMode = myAudioManager.getMode();

        myAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        // change mic state only if needed
        if (myAudioManager.isMicrophoneMute() != state) {
            myAudioManager.setMicrophoneMute(state);
        }

        // set back the original working mode
        myAudioManager.setMode(workingAudioMode);
    }

}
