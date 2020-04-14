package br.ufabc.gravador.controls.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DirectoryHelper {

    private static final String MY_PROVIDER = "br.ufabc.gravador.fileprovider";
    private Context context;

    public static final int GRAVACAO_DIR = 1, AUDIO_DIR = 2, VIDEO_DIR = 3;
    private static final String GRAVACAO_PATH = "Gravacao", AUDIO_PATH = "Audio", VIDEO_PATH = "Video";
    private File gravacaoDir, audioDir, videoDir;

    public DirectoryHelper ( Context context ) {
        this.context = context;
        setup();
    }

    public static String newTempName () {
        return new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault())
                .format(new Date());
    }

    public static String getMimeTypeFromExtension ( String extension ) {
        String valid = extension.substring(extension.lastIndexOf('.') + 1).toLowerCase();
        String mimeType = "*/*";
        if ( MimeTypeMap.getSingleton().hasExtension(valid) )
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(valid);
        return mimeType;
    }

    private void setup () {
        File ext = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Registro de Aulas");

        gravacaoDir = new File(ext, GRAVACAO_PATH);
        if ( !gravacaoDir.exists() )
            gravacaoDir.mkdirs();

        audioDir = new File(ext, AUDIO_PATH);
        if ( !audioDir.exists() )
            audioDir.mkdirs();

        videoDir = new File(ext, VIDEO_PATH);
        if ( !videoDir.exists() )
            videoDir.mkdirs();
    }

    public File getDirectory ( int dir ) {
        switch ( dir ) {
            case GRAVACAO_DIR:
                return gravacaoDir;
            case AUDIO_DIR:
                return audioDir;
            case VIDEO_DIR:
                return videoDir;
            default:
                return null;
        }
    }

    public List<File> listFiles ( int dir, FilenameFilter filter ) {
        String subdir = ".";
        switch ( dir ) {
            case GRAVACAO_DIR:
                subdir = GRAVACAO_PATH;
                break;
            case AUDIO_DIR:
                subdir = AUDIO_PATH;
                break;
            case VIDEO_DIR:
                subdir = VIDEO_PATH;
                break;
        }
        List<File> allFiles = new ArrayList<File>();

        File ext = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Registro de Aulas");
        File sdir = new File(ext, subdir);

        File[] matches = sdir.listFiles(filter);
        if ( matches != null )
            allFiles.addAll(Arrays.asList(matches));

        return allFiles;
    }

    public File createFile ( int dir, String name ) {
        File f = new File(getDirectory(dir), name);
        try {
            f.createNewFile();
        } catch ( IOException e ) {
            return null;
        }
        return f;
    }

    public Uri createURIFromString ( String location ) {
        Uri uri;
        try {
            File f = new File(location);
            f.createNewFile();
            uri = FileProvider.getUriForFile(context, MY_PROVIDER, f);
        } catch ( IOException e ) {
            Log.e("RegistroDeAulas", "Could not open file", e);
            return null;
        }
        return uri;
    }

    public ParcelFileDescriptor openFdFromURI ( Uri uri ) {
        ParcelFileDescriptor fd;
        try {
            ContentResolver cr = context.getContentResolver();
            fd = cr.openFileDescriptor(uri, "rw");
        } catch ( IOException e ) {
            Log.e("RegistroDeAulas", "Could not open file", e);
            return null;
        }
        return fd;
    }

    public ParcelFileDescriptor createFdFromString ( String location ) {
        Uri uri = createURIFromString(location);
        return uri == null ? null : openFdFromURI(uri);
    }

    public ParcelFileDescriptor openFdFromString ( String location ) {
        Uri uri = Uri.parse(location);
        return uri == null ? null : openFdFromURI(uri);
    }

    public void deleteFileFromURI ( String location ) {
        Uri uri = Uri.parse(location);
        ContentResolver cr = context.getContentResolver();
        cr.delete(uri, null, null);
    }
}
