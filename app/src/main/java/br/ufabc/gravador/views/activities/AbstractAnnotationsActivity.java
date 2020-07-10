package br.ufabc.gravador.views.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import br.ufabc.gravador.controls.helpers.DirectoryHelper;
import br.ufabc.gravador.models.Gravacao;
import br.ufabc.gravador.views.fragments.AnnotationsFragment;

public abstract class AbstractAnnotationsActivity extends AbstractServiceActivity implements AnnotationsFragment.AnnotationFragmentListener {

    protected AnnotationsFragment annotationsFragment = null;
    protected Gravacao gravacao = null;
    protected AnnotationsFragment.PictureListener pictureListener = null;
    protected Uri pictureURI;

    @Override
    protected void onServiceOnline() {
        annotationsFragment.updateGravacao();
    }

    @Override
    public void receiveFragment(AnnotationsFragment f) {
        annotationsFragment = f;
    }

    public int getGravacaoTime() {
        return !isBound ? 0 : (int) gravacaoService.getTime();
    }

    @Override
    public Gravacao getGravacao() {
        return gravacao;
    }

    public void takePicture(AnnotationsFragment.PictureListener listener) {
        pictureListener = listener;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Anexar foto")
                .setItems(new String[]{"Tirar foto", "Escolher imagem"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            DirectoryHelper dh = new DirectoryHelper(AbstractAnnotationsActivity.this);
                            Uri uri = dh.createURIFromString(
                                    dh.createFile(
                                            DirectoryHelper.PICTURE_DIR, DirectoryHelper.newTempName() + ".jpg"
                                    ).getAbsolutePath());
                            pictureURI = uri;
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                            startActivityForResult(intent, 10203);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, 30201);
                        }
                    }
                }).create();

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 30201 || requestCode == 10203) && resultCode == RESULT_OK && data != null) {
            if (requestCode == 30201)
                pictureURI = data.getData();
            if (pictureListener != null) {
                pictureListener.onPictureFound(pictureURI);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        annotationsFragment.alertSave(() -> {
            gravacaoService.saveGravacao(null);
        });
        //TODO SALVAR MESMO??????
    }

}
