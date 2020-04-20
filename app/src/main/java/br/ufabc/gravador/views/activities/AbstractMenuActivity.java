package br.ufabc.gravador.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;

import br.ufabc.gravador.R;

public abstract class AbstractMenuActivity extends AppCompatActivity {

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

    void menuConfigOnClick(View view) {
        //TODO
    }

    void menuLibrasOnClick(View view) {
        //TODO
    }
}
