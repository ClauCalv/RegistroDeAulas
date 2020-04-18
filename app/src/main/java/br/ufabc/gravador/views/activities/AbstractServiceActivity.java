package br.ufabc.gravador.views.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.core.widget.ContentLoadingProgressBar;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.services.GravacaoService;

public abstract class AbstractServiceActivity extends AbstractMenuActivity {

    protected GravacaoService gravacaoService;
    protected ServiceConnection serviceConnection;
    protected boolean isBound = false;
    private boolean superCalled = false;

    private ContentLoadingProgressBar progressBar;

    @CallSuper
    protected void onSuperCreate(Bundle savedInstanceState) {
        superCalled = true;

        ViewGroup rootView = (ViewGroup) ( (ViewGroup) findViewById(
                android.R.id.content) ).getChildAt(0);
        getLayoutInflater().inflate(R.layout.content_loading_progress_bar, rootView, true);
        progressBar = rootView.findViewById(R.id.server_progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!superCalled)
            throw new UnsupportedOperationException("Must call \"super.onSuperCreate\"!");
    }

    @Override
    protected void onResume () {
        super.onResume();

        progressBar.show();
        Intent intent = new Intent(this, GravacaoService.class);
        startService(intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected ( ComponentName className, IBinder binder ) {
                gravacaoService = ( (GravacaoService.LocalBinder) binder ).getService();
                isBound = true;
                onServiceOnline();
                progressBar.hide();
            }

            @Override
            public void onServiceDisconnected ( ComponentName arg0 ) {
                isBound = false;
            }
        };
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

    }

    @Override
    protected void onPause () {
        super.onPause();
        if ( serviceConnection != null ) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    protected abstract void onServiceOnline ();

}
