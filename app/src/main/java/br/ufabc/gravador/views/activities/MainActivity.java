package br.ufabc.gravador.views.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import br.ufabc.gravador.BuildConfig;
import br.ufabc.gravador.R;

public class MainActivity extends AbstractMenuActivity {

    Button initRecord, joinHostRecord, viewRecords;

    @Override
    protected int getLayoutID() {
        return R.layout.activity_main;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {

        initRecord = findViewById(R.id.initRecord);
        initRecord.setOnClickListener(this::initRecordOnClick);

        joinHostRecord = findViewById(R.id.joinHostRecord);
        joinHostRecord.setOnClickListener(this::joinHostRecordOnClick);

        viewRecords = findViewById(R.id.viewRecords);
        viewRecords.setOnClickListener(this::viewRecordsOnClick);

    }

    void initRecordOnClick ( View view ) {
        Intent intent = new Intent(this, NewRecordActivity.class);
        startActivity(intent);
    }

    void joinHostRecordOnClick ( View view ) {
        Intent intent = new Intent(this, SharedRecordActivity.class);
        startActivity(intent);
    }

    void viewRecordsOnClick ( View view ) {
        Intent intent = new Intent(this, ViewGravacoesActivity.class);
        startActivity(intent);
    }

    private void checkFirstRun() {

        final String PREFS_NAME = BuildConfig.APPLICATION_ID;
        final String PREF_VERSION_CODE_KEY = "version_code";
        final int DOESNT_EXIST = -1;

        int currentVersionCode = BuildConfig.VERSION_CODE;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);

        if (currentVersionCode == savedVersionCode)
            return;

        if (savedVersionCode == DOESNT_EXIST) {
            // TODO This is a new install (or the user cleared the shared preferences)

        } else if (currentVersionCode > savedVersionCode) {
            // TODO This is an upgrade
        }

        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).apply();
    }
}
