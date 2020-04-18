package br.ufabc.gravador.models;

import android.content.Context;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import br.ufabc.gravador.controls.helpers.DirectoryHelper;

public class GravacaoManager {

    private Context context;
    private DirectoryHelper dh;
    private SaveListener listener;

    public GravacaoManager(Context context) {
        this.context = context;
        this.dh = new DirectoryHelper(context);
    }

    public GravacaoManager(Context context, DirectoryHelper dh) {
        this.context = context;
        this.dh = dh;
    }

    public Gravacao CreateEmpty ( String annotationName ) {
        Gravacao g = new Gravacao();
        g.annotationName = annotationName;
        g.lastSaved = false;

        File f = new File(dh.getDirectory(DirectoryHelper.GRAVACAO_DIR),
                annotationName + Gravacao.annotationExtension);
        g.annotationURI = dh.createURIFromString(f.toString()).toString();
        return g;
    }

    public Gravacao CreateFromXML ( String annotationURI ) {
        Gravacao gravacao = new Gravacao();
        ParcelFileDescriptor fd = dh.openFdFromString(annotationURI);
        InputStream inputStream = new FileInputStream(fd.getFileDescriptor());

        if ( !gravacao.loadXML(inputStream) ) return null;

        gravacao.annotationURI = annotationURI;
        gravacao.lastSaved = true;
        return gravacao;
    }

    public void setSaveListener ( SaveListener listener ) { this.listener = listener; }

    public SaveListener getSaveListener () {return listener;}

    public void saveGravacao ( Gravacao gravacao ) {
        FileDescriptor fd = dh.openFdFromString(gravacao.getAnnotationURI()).getFileDescriptor();
        new AsyncSave(gravacao, fd, listener).execute();
    }

    public boolean renameAndSave ( Gravacao gravacao, String annotationName ) {
        if ( !annotationName.equals(gravacao.annotationName) ) {
            File f = new File(dh.getDirectory(DirectoryHelper.GRAVACAO_DIR),
                    annotationName + Gravacao.annotationExtension);
            if ( f.exists() ) return false;
            dh.deleteFileFromURI(gravacao.getAnnotationURI());
            gravacao.annotationURI = dh.createURIFromString(f.toString()).toString();
            gravacao.annotationName = annotationName;
        }
        saveGravacao(gravacao);
        return true;
    }

    public void removeRecordAndSave ( Gravacao gravacao ) {
        gravacao.recordURI = null;
        saveGravacao(gravacao);
    }

    public interface SaveListener {
        void onGravacaoSaved ( boolean success );
    }

    private static class AsyncSave extends AsyncTask<Void, Void, Boolean> {

        private SaveListener listener;
        private StringWriter writer;
        private FileDescriptor fd;
        private FileOutputStream fos;
        private Gravacao g;

        public AsyncSave ( Gravacao g, FileDescriptor fd, @Nullable SaveListener listener ) {
            this.g = g;
            this.fd = fd;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute () {
            writer = new StringWriter();
            fos = new FileOutputStream(fd);
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            if (!g.saveXML(writer)) return false;

            try {
                fos.write(writer.toString().getBytes());
                fos.close();
            } catch ( IOException e ) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute ( Boolean success ) {
            if ( success ) g.lastSaved = true;
            if ( listener != null ) listener.onGravacaoSaved(success);
        }
    }
}