package br.ufabc.gravador.views.activities;

import android.os.Bundle;

import br.ufabc.gravador.R;

//TODO extend abstractMenuActivity
public class AnnotationOnTransmissionActivity extends AbstractServiceActivity {

    @Override
    protected int getLayoutID() {
        return R.layout.activity_annotation_on_transmission;
    }

    @Override
    protected void onSuperCreate(Bundle savedInstanceState) {
        super.onSuperCreate(savedInstanceState);
        //TODO
    }

    @Override
    protected void onServiceOnline() {
        //TODO
    }
}
