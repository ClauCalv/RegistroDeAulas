package br.ufabc.gravador.models;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Locale;

public class Gravacao {

    public static final String annotationExtension = ".grv.xml";
    protected String recordURI, annotationURI, annotationName;

    protected SparseArray<Annotations> annotations;
    protected boolean lastSaved, isSaving = false;
    private int lastAnnotationID = 0;

    protected Gravacao () {
        annotations = new SparseArray<Annotations>();
        this.annotationName = annotationName;
    }

    public static String formatTime ( long time ) {
        long hh = time / ( 1000 * 60 * 60 ), mm = time / ( 1000 * 60 ) % 60, ss = time / 1000 % 60;
        return hh > 0
                ? String.format(Locale.getDefault(), "%d:%02d:%02d", hh, mm, ss)
                : String.format(Locale.getDefault(), "%02d:%02d", mm, ss);
    }

    public String getName () { return annotationName; }

    public String getAnnotationURI () { return annotationURI; }

    public void setRecordURI ( String recordURI ) { this.recordURI = recordURI; }

    public String getRecordURI () { return recordURI; }

    public boolean isLastSaved () { return lastSaved; }

    public boolean isSaving () { return isSaving; }

    public boolean hasRecord () { return recordURI != null; }

    public boolean hasAnnotation () { return annotations.size() != 0; }

    public int getAnnotationCount () { return annotations.size(); }

    protected boolean loadXML ( InputStream inputStream ) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.nextTag();

            annotations.clear();
            Annotations a = null;
            int eventType = parser.getEventType();
            while ( eventType != XmlPullParser.END_DOCUMENT ) {
                if ( eventType == XmlPullParser.START_TAG ) {
                    if ( parser.getName().equals("Name") ) {
                        if ( parser.next() == XmlPullParser.TEXT ) {
                            annotationName = parser.getText();
                        }
                    } else if ( parser.getName().equals("Record") ) {
                        if ( parser.next() == XmlPullParser.TEXT ) {
                            recordURI = parser.getText();
                        }
                    } else if ( parser.getName().equals("Annotation") ) {
                        int id = Integer.valueOf(parser.getAttributeValue(null, "ID"));
                        String name = parser.getAttributeValue(null, "name");
                        int milissec = Integer.valueOf(parser.getAttributeValue(null, "time"));
                        a = addAnnotation(milissec, id, name);
                    } else if ( parser.getName().equals("Content") ) {
                        String type = parser.getAttributeValue(null, "contentType");
                        if ( parser.next() == XmlPullParser.TEXT ) {
                            if ( a == null ) throw new XmlPullParserException("null annotation");
                            String text = parser.getText();
                            if ( type.equals(text) ) a.setText(text);
                            else if ( type.equals("image") ) a.addImage(text);
                        }
                    }
                } else if ( eventType == XmlPullParser.END_TAG )
                    if ( parser.getName().equals("Annotation") ) a = null;

                eventType = parser.next();
            }
            Log.i("xml", "Load successful");

            lastAnnotationID = annotations.size() > 0
                    ? annotations.keyAt(annotations.size() - 1)
                    : 0;
            return true;

        } catch ( XmlPullParserException e ) {
            Log.e("xml", "XML malformed", e);
        } catch ( IOException e ) {
            Log.e("xml", "IO Exception", e);
        }
        return false;
    }

    public boolean saveXML ( StringWriter writer, boolean saveRecord, boolean saveAnnotations ) {
        isSaving = true;
        try {
            Log.wtf("saveXML",
                    saveRecord + ", " + hasRecord() + ", " + saveAnnotations + ", " + hasAnnotation());
            XmlSerializer xmlSerializer = Xml.newSerializer();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.startTag(null, "doc");

            xmlSerializer.startTag(null, "Name");
            xmlSerializer.text(annotationName);
            xmlSerializer.endTag(null, "Name");

            if ( saveRecord && hasRecord() ) {
                xmlSerializer.startTag(null, "Record");
                xmlSerializer.text(getRecordURI());
                xmlSerializer.endTag(null, "Record");
            }

            if ( saveAnnotations && hasAnnotation() ) {
                xmlSerializer.startTag(null, "Annotations");
                for ( int i = 0; i < annotations.size(); i++ )
                    annotations.valueAt(i).saveAnnotation(xmlSerializer);
                xmlSerializer.endTag(null, "Annotations");
            }

            xmlSerializer.endTag(null, "doc");
            xmlSerializer.endDocument();
            xmlSerializer.flush();

            isSaving = false;
            return true;

        } catch ( IllegalArgumentException | IllegalStateException e ) {
            Log.e("xml", "XML malformed", e);
        } catch ( IOException e ) {
            Log.e("xml", "IO Exception", e);
        }
        isSaving = false;
        return false;
    }

    public Annotations addAnnotation ( int milissec, String name ) {
        Annotations a = new Annotations(milissec, name);
        annotations.append(a.id, a);
        lastSaved = false;
        return a;
    }

    private Annotations addAnnotation ( int milissec, int id, String name ) {
        Annotations a = new Annotations(milissec, id, name);
        annotations.append(a.id, a);
        lastSaved = false;
        return a;
    }

    public Annotations getAnnotation ( int id ) {
        return annotations.get(id);
    }

    public Annotations getAnnotationOnPos ( int pos ) {
        if ( pos >= annotations.size() ) return null;
        return annotations.valueAt(pos);
    }

    public int getAnnotationIDOnPos ( int pos ) {
        if ( pos >= annotations.size() ) return -1;
        return annotations.keyAt(pos);
    }

    public int getAnnotationIDOnTime ( int millisec ) {
        for ( int i = 0; i < annotations.size(); i++ )
            if ( annotations.valueAt(i).getTime() == millisec )
                return annotations.keyAt(i);
        return -1;
    }

    public int[] getAnnotationTimes () {
        int[] times = new int[annotations.size()];
        for ( int i = 0; i < annotations.size(); i++ )
            times[i] = annotations.valueAt(i).getTime();
        return times;
    }

    public void deleteAnnotation ( int id ) {
        annotations.delete(id);
        lastSaved = false;
    }

    public void setAnnotationName ( int id, String name ) {
        Annotations a = annotations.get(id);
        if ( a == null ) return;

        a.setName(name);
        lastSaved = false;
    }

    public void setAnnotationText ( int id, String text ) {
        Annotations a = annotations.get(id);
        if ( a == null ) return;

        a.setText(text);
        lastSaved = false;
    }

    //    public void setAnnotationImage ( int id, String uri ) {
    //        Annotations a = annotations.get(id);
    //        if ( a == null ) return;
    //
    //        a.addImage(uri);
    //        lastSaved = false;
    //    }

    public class Annotations implements Serializable {

        public final int id;

        private String name, text, imageUri;
        private int time;

        transient private Bitmap icon;
        private int iconW = -1, iconH = -1;

        protected Annotations ( int millissec, String name ) {
            this(millissec, ++lastAnnotationID, name);
        }

        /**
         * UNSAFE
         */
        private Annotations ( int millissec, int id, String name ) {
            this.time = millissec;
            this.name = name;
            this.id = id;
            if ( id > lastAnnotationID ) lastAnnotationID = id;
        }

        public int getTime () { return time; }

        public String getTimeStamp () {
            return formatTime(time);
        }

        public String getName () { return name; }

        protected void setName ( String name ) { this.name = name; }

        public String getText () { return text; }

        protected void setText ( String text ) {
            this.text = text;
        }

        public boolean hasText () { return text != null; }

        public String getImageUri () { return imageUri; }

        protected void addImage ( String imageUri ) {
            this.imageUri = imageUri;
        }

        public boolean hasImage () {
            return imageUri != null;
        }

        //        public Bitmap getIcon ( int targetW, int targetH ) {
        //            if ( imagePath == null ) return null;
        //
        //            if ( targetW <= 0 || targetH <= 0 ) return null;
        //
        //            if ( icon != null && targetW == iconW && targetH == iconH ) return icon;
        //
        //            // Get the dimensions of the bitmap
        //            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        //            bmOptions.inJustDecodeBounds = true;
        //            BitmapFactory.decodeFile(imagePath, bmOptions);
        //
        //            int photoW = bmOptions.outWidth;
        //            int photoH = bmOptions.outHeight;
        //
        //            // Determine how much to scale down the image
        //            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        //
        //            // Decode the image file into a Bitmap sized to fill the View
        //            bmOptions.inJustDecodeBounds = false;
        //            bmOptions.inSampleSize = scaleFactor;
        //            bmOptions.inPurgeable = true;
        //
        //            icon = BitmapFactory.decodeFile(imagePath, bmOptions);
        //            return icon;
        //        }

        protected void saveAnnotation ( XmlSerializer xml ) throws IOException {
            xml.startTag(null, "Annotation");
            xml.attribute(null, "ID", String.valueOf(id));
            xml.attribute(null, "name", name);
            xml.attribute(null, "time", String.valueOf(getTime()));
            xml.attribute(null, "timestamp", getTimeStamp());

            if ( hasText() ) {
                xml.startTag(null, "Content");
                xml.attribute(null, "contentType", "text");
                xml.text(text);
                xml.endTag(null, "Content");
            }

            if ( hasImage() ) {
                xml.startTag(null, "Content");
                xml.attribute(null, "contentType", "image");
                xml.text(imageUri);
                xml.endTag(null, "Content");
            }

            xml.endTag(null, "Annotation");
        }
    }
}
