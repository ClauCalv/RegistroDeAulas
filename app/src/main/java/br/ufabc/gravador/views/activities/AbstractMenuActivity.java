package br.ufabc.gravador.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;

import java.util.List;

import br.ufabc.gravador.R;

public abstract class AbstractMenuActivity extends AppCompatActivity {

    private static final String moduloConfigurador = "ufabc.projeto.moduloconfigurador";

    protected Toolbar myToolbar;
    protected ActionBar myActionBar;

    protected View activityView;
    protected Group menuGroup;

    protected ImageButton menuHelp, menuConfig, menuLibras;

    protected abstract void onSuperCreate(@Nullable Bundle savedInstanceState);

    @LayoutRes
    protected abstract int getLayoutID();

    /*
     * Child class should not call this method via super, especially in the onSuperCreate
     */
    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int LayoutID = getLayoutID();
        setContentView(R.layout.activity_abstract_menu);

        myToolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(myToolbar);
        myActionBar = getSupportActionBar();
        myActionBar.setDisplayShowHomeEnabled(false);
        myActionBar.setDisplayShowTitleEnabled(false);
        myActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
        myActionBar.setDisplayShowCustomEnabled(true);

        FrameLayout fl = findViewById(R.id.activity_placeholder);
        activityView = LayoutInflater.from(this).inflate(LayoutID, fl, true);

        menuHelp = findViewById(R.id.menu_help);
        menuHelp.setOnClickListener(this::menuHelpOnClick);

        menuConfig = findViewById(R.id.menu_config);
        menuConfig.setOnClickListener(this::menuConfigOnClick);

        menuLibras = findViewById(R.id.menu_libras);
        menuLibras.setOnClickListener(this::menuLibrasOnClick);

        menuGroup = findViewById(R.id.group_menu_itens);

        onSuperCreate(savedInstanceState);
    }

    void menuHelpOnClick(View view) {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public static boolean isPackageInstalled(@NonNull final Context context, @NonNull final String targetPackage) {
        List<ApplicationInfo> packages = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (targetPackage.equals(packageInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    void menuLibrasOnClick(View view) {
        //TODO
    }

    void menuConfigOnClick(View view) {
        Intent intent;
        if (isPackageInstalled(this, moduloConfigurador)) {
            intent = getPackageManager().getLaunchIntentForPackage(moduloConfigurador);
        } else {
            intent = new Intent(this, BasicConfigActivity.class);
        }
        startActivity(intent);
    }
}
