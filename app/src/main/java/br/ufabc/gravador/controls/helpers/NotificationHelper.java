package br.ufabc.gravador.controls.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;
import br.ufabc.gravador.views.activities.OpenAudioActivity;
import br.ufabc.gravador.views.activities.RecordAudioActivity;

public class NotificationHelper {

    public static String CHANNEL_DEFAULT_IMPORTANCE = "Default";
    public static int RECORD_REQUEST_CODE = 6001, SAVE_REQUEST_CODE = 6002, PLAY_REQUEST_CODE = 6003;
    private static int NOTIFICATION_ICON = R.drawable.ic_stat_name;

    public static int NO_FLAG = 0;

    private Context context;

    public NotificationHelper ( Context context ) {
        this.context = context;
        createNotificationChannel();
    }

    public PendingIntent buildRecordPendingIntent () {
        Intent intent = new Intent(context, RecordAudioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, RECORD_REQUEST_CODE, intent, NO_FLAG);
    }

    public PendingIntent buildSavePendingIntent () {
        Intent intent = new Intent(context, RecordAudioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, SAVE_REQUEST_CODE, intent, NO_FLAG);
    }

    public PendingIntent buildPlayAudioPendingIntent () {
        Intent intent = new Intent(context, OpenAudioActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, PLAY_REQUEST_CODE, intent, NO_FLAG);
    }

    private PendingIntent buildGravacaoServiceButtonPendingIntent ( String action ) {
        Intent intent = new Intent(context, GravacaoService.class);
        intent.putExtra(GravacaoService.SERVICE_ACTION, action);
        return PendingIntent.getActivity(context, PLAY_REQUEST_CODE, intent, NO_FLAG);
    }

    public void pushNotification ( int ID, Notification notification ) {
        NotificationManagerCompat.from(context).notify(ID, notification);
    }

    public void clearNotifications ( int ID ) {
        NotificationManagerCompat.from(context).cancel(ID);
    }

    public Notification newSaveNotification ( PendingIntent pendingIntent ) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                CHANNEL_DEFAULT_IMPORTANCE)
                .setSmallIcon(NOTIFICATION_ICON)//TODO
                .setContentTitle("Gravação encerrada") //TODO hardcoded
                .setContentText("Toque para salvar a gravação") //TODO hardcoded
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);
        return notification.build();
    }

    public Notification newPlayAudioNotification ( PendingIntent pendingIntent, String name, boolean isPlaying ) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                CHANNEL_DEFAULT_IMPORTANCE)
                .setSmallIcon(NOTIFICATION_ICON)//TODO
                .setContentTitle("Reproduzindo " + name) //TODO hardcoded
                .setContentText("Toque para editar a gravação") //TODO hardcoded
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .addAction(
                        android.R.drawable.ic_media_previous,
                        "Marcação Anterior",
                        buildGravacaoServiceButtonPendingIntent(GravacaoService.ACTION_PREV))
                .addAction(
                        isPlaying
                                ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        isPlaying ? "Pausar" : "Reproduzir",
                        buildGravacaoServiceButtonPendingIntent(GravacaoService.ACTION_PLAY_PAUSE))
                .addAction(
                        android.R.drawable.ic_media_next,
                        "Próxima Marcação",
                        buildGravacaoServiceButtonPendingIntent(GravacaoService.ACTION_NEXT))
                .setAutoCancel(false);
        return notification.build();
    }

    public Notification newRecordAudioNotification ( PendingIntent pendingIntent ) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                CHANNEL_DEFAULT_IMPORTANCE)
                .setSmallIcon(NOTIFICATION_ICON)//TODO
                .setContentTitle("Gravação em andamento") //TODO hardcoded
                .setContentText("Toque para editar a gravação") //TODO hardcoded
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);
        return notification.build();
    }

    public Notification newBlankNotification () {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                CHANNEL_DEFAULT_IMPORTANCE)
                .setSmallIcon(NOTIFICATION_ICON)//TODO
                .setContentTitle("Registro de Aulas está aberto") //TODO hardcoded
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false);
        return notification.build();
    }

    private void createNotificationChannel () {

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            CharSequence name = "Playing/Recording"; //TODO hardcoded
            String description = "Notification for foreground service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT_IMPORTANCE, name,
                    importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(
                    NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
