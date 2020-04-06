package br.ufabc.gravador.controls.helpers;

import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyFileManager {

    public static final int GRAVACAO_DIR = 1, AUDIO_DIR = 2, VIDEO_DIR = 3;
    private static final int MAX_FILES_PER_DIR = 2000;
    private static final String GRAVACAO_PATH = "Gravacao", AUDIO_PATH = "Audio", VIDEO_PATH = "Video";
    private static MyFileManager instance = null;
    private File gravacaoDir, audioDir, videoDir;
    private boolean isSetup = false;
    private Context context;

    private MyFileManager () {} //FIXME

    public static MyFileManager getInstance () {
        if ( instance == null ) instance = new MyFileManager();
        return instance;
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

    /*
     * Failing a setup will invalidate previous setups
     */
    public boolean setup ( Context ctx ) {
        isSetup = false;

        context = ctx;

        File ext = context.getExternalFilesDir(null);

        gravacaoDir = new File(ext, GRAVACAO_PATH);
        if ( !gravacaoDir.exists() )
            if ( !gravacaoDir.mkdirs() )
                return false;

        audioDir = new File(ext, AUDIO_PATH);
        if ( !audioDir.exists() )
            if ( !audioDir.mkdirs() )
                return false;

        videoDir = new File(ext, VIDEO_PATH);
        if ( !videoDir.exists() )
            if ( !videoDir.mkdirs() )
                return false;

        isSetup = true;
        return true;
    }

    public File getDirectory ( int dir ) {
        if ( isSetup )
            switch ( dir ) {
                case GRAVACAO_DIR:
                    return gravacaoDir;
                case AUDIO_DIR:
                    return audioDir;
                case VIDEO_DIR:
                    return videoDir;
            }
        Log.wtf("ISSETUP", "setup = false?");
        return null;
    }

    public List<File> listFiles ( int dir, FilenameFilter filter ) {
        if ( !isSetup )
            return null;

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

        File[] ext = context.getExternalFilesDirs(null);
        List<File> allFiles = new ArrayList<File>();

        for ( File f : ext ) {
            File subf = new File(f, subdir);
            File[] matches = subf.listFiles(filter);
            if ( matches != null )
                allFiles.addAll(Arrays.asList(matches));
        }

        return allFiles;
    }

}
