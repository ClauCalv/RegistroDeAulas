package br.ufabc.gravador.controls.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

public class TimerHelper {

    private final Context context;
    private TimeUpdateListener registeredTimeListener;
    private long currentTime = 0, runnableStartTime = 0, timetotal = 0;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            currentTime = SystemClock.uptimeMillis() - runnableStartTime;
            timeHandler.postDelayed(this, timetotal > 20000 ? 500 : 200); //reduce precision
            onTimeUpdate();
        }
    };

    public TimerHelper(Context context) {
        this.context = context;
    }

    private void onTimeUpdate() {
        if (registeredTimeListener != null) registeredTimeListener.onTimeUpdate(currentTime);
    }

    public void setTimeUpdateListener(TimeUpdateListener listener) {
        registeredTimeListener = listener;
    }

    public void startTimer(long offset) {
        runnableStartTime = SystemClock.uptimeMillis() - offset;
        timeHandler.postDelayed(timeRunnable, 0);
    }

    public void stopTimer() {
        timeHandler.removeCallbacks(timeRunnable);
    }

    public void resetTimer() {
        currentTime = 0;
    }

    public long getTime() {
        return currentTime;
    }

    public long getTimeTotal() {
        return timetotal;
    }

    public void setTimeTotal(long timetotal) {
        this.timetotal = timetotal;
    }

    public interface TimeUpdateListener {
        void onTimeUpdate(long time);
    }
}
