package br.ufabc.gravador.views.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.ufabc.gravador.R;
import br.ufabc.gravador.models.Gravacao;

public class AnnotationsFragment extends Fragment {

    protected Gravacao gravacao;
    private int selectedID = -1;

    private Spinner annotationsSelector;
    private EditText annotationContent, annotationName;
    private ImageButton annotationNewButton, annotationTakePicture, annotationDelete;
    private Button annotationSave;
    private TextView annotationTime;
    private BaseAdapter adapter;
    private ImageView annotationImage;
    private AnnotationFragmentListener activityListener;
    private ViewGroup selectionPane, contentPane, imagePane;
    private boolean hasTextChanged = false, textEmpty = true, imagePreview = false;

    public AnnotationsFragment () {
        // Required empty public constructor
    }

    public static AnnotationsFragment getInstance () {
        AnnotationsFragment fragment = new AnnotationsFragment();
        return fragment;
    }

    @Override
    public void onAttach ( Context context ) {
        super.onAttach(context);
        if ( context instanceof AnnotationFragmentListener ) {
            activityListener = (AnnotationFragmentListener) context;
        } else {
            throw new RuntimeException(
                    context.toString() + " must implement AnnotationFragmentListener");
        }
    }

    @Override
    public void onDetach () {
        super.onDetach();
        activityListener = null;
    }

    @Override
    public void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        //TODO?
    }

    @Override
    public View onCreateView ( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_annotations, container, false);
    }

    @Override
    public void onActivityCreated ( @Nullable Bundle savedInstanceState ) {
        super.onActivityCreated(savedInstanceState);
        View master = getView();

        annotationsSelector = master.findViewById(R.id.annotationsSelector);
        adapter = new BaseAdapter() {
            @Override
            public int getCount () {
                return gravacao == null
                        ? 1
                        : gravacao.hasAnnotation() ? gravacao.getAnnotationCount() : 1;
            }

            @Override
            public Object getItem ( int i ) {
                return gravacao == null
                        ? null
                        : gravacao.getAnnotationOnPos(i);
            }

            @Override
            public long getItemId ( int i ) {
                return gravacao == null
                        ? -1
                        : gravacao.getAnnotationIDOnPos(i);
            }

            public View getView ( int position, View convertView, ViewGroup parent, boolean isDropDown ) {
                ViewHolder holder;
                if ( convertView == null ) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(R.layout.spinner_item, null);
                    holder = new ViewHolder();
                    holder.txtview = convertView.findViewById(R.id.spinner_textitem);
                    convertView.setTag(holder);
                } else holder = (ViewHolder) convertView.getTag();

                holder.txtview.setText(gravacao == null || !gravacao.hasAnnotation()
                        ? "SEM ANOTAÇÕES"
                        : isDropDown || selectedID == -1
                                ? gravacao.getAnnotationOnPos(position).getName()
                                : gravacao.getAnnotation(selectedID).getName());
                return convertView;
            }

            @Override
            public View getView ( int position, View convertView, ViewGroup parent ) {
                return getView(position, convertView, parent, false);
            }

            @Override
            public View getDropDownView ( int position, View convertView, ViewGroup parent ) {
                return getView(position, convertView, parent, true);
            }

            class ViewHolder {
                private TextView txtview;
            }
        };
        annotationsSelector.setAdapter(adapter);
        annotationsSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected ( AdapterView<?> adapterView, View view, int i, long l ) {
                selectAnnotation((int) l);
            }

            @Override
            public void onNothingSelected ( AdapterView<?> adapterView ) { }
        });

        annotationContent = master.findViewById(R.id.annotationContent);
        annotationContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged ( CharSequence charSequence, int i, int i1, int i2 ) { }

            @Override
            public void onTextChanged ( CharSequence charSequence, int i, int i1, int i2 ) {
                if ( i1 != 0 || i2 != 0 ) {
                    hasTextChanged = true;
                }
            }

            @Override
            public void afterTextChanged ( Editable editable ) {}
        });
        //annotationContent.setEnabled(false);

        annotationName = master.findViewById(R.id.annotationName);
        annotationName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged ( CharSequence s, int start, int count, int after ) { }

            @Override
            public void onTextChanged ( CharSequence s, int start, int before, int count ) { }

            @Override
            public void afterTextChanged ( Editable s ) {
                hasTextChanged = true;
            }
        });

        annotationNewButton = master.findViewById(R.id.annotationNewButton);
        annotationNewButton.setOnClickListener(this::newButtonOnClick);

        annotationTakePicture = master.findViewById(R.id.annotationTakePicture);
        annotationTakePicture.setOnClickListener(this::takePictureOnClick);

        annotationSave = master.findViewById(R.id.annotationSave);
        annotationSave.setOnClickListener(this::saveOnClick);

        annotationDelete = master.findViewById(R.id.annotationDelete);
        annotationDelete.setOnClickListener(this::deleteOnClick);

        annotationTime = master.findViewById(R.id.annotationTime);
        annotationTime.setText("00:00");

        annotationImage = master.findViewById(R.id.annotationImage);
        annotationImage.setOnClickListener(this::imageOnClick);

        selectionPane = master.findViewById(R.id.selection_pane);
        contentPane = master.findViewById(R.id.content_pane);
        imagePane = master.findViewById(R.id.image_pane);

        gravacao = activityListener.getGravacao();
        activityListener.receiveFragment(this);

        adapter.notifyDataSetChanged();
        hasTextChanged = false;
    }

    public void updateGravacao() {
        gravacao = activityListener.getGravacao();
    }

    public interface annotationSavedListener {
        void onAnnotationSaved ();
    }

    public void alertSave ( annotationSavedListener listener ) {
        if (hasTextChanged && selectedID != -1)
            new AlertDialog.Builder(getContext()).setMessage(
                    "Salvar mudanças na anotação anterior?")
                    .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == AlertDialog.BUTTON_POSITIVE) saveOnClick(null);
                            listener.onAnnotationSaved();
                        }
                    })
                    .setNegativeButton("Descartar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            listener.onAnnotationSaved();
                        }
                    })
                    .show();
        else listener.onAnnotationSaved();
    }

    public boolean openAnnotation(int ID) {
        if (ID == -1) return false;
        selectedID = ID;
        Gravacao.Annotations a = gravacao.getAnnotation(selectedID);
        annotationName.setText(a.getName());
        annotationTime.setText(a.getTimeStamp());
        annotationContent.setEnabled(true);
        annotationContent.setText(a.getText());
        hasTextChanged = false;
        adapter.notifyDataSetChanged();
        activityListener.onAnnotationChanged(ID, true);
        return a.hasImage();
    }

    public void selectAnnotation(int ID) {
        if ( ID == -1 || ID == selectedID ) return;

        alertSave(() -> {
            boolean has_images = openAnnotation(ID);
            toggleContent(true);
            toggleImage(has_images);
            if (has_images) loadAnnotationImage();
        });
    }

    public void loadAnnotationImage() {
        String uri = gravacao.getAnnotation(selectedID).getImageUri();
        annotationImage.setImageURI(Uri.parse(uri));
    }

    public void saveAnnotation() {
        gravacao.setAnnotationName(selectedID, annotationName.getText().toString());
        gravacao.setAnnotationText(selectedID, annotationContent.getText().toString());
        adapter.notifyDataSetChanged();
        hasTextChanged = false;
    }

    private void deleteAnnotation() {
        final int time = gravacao.getAnnotation(selectedID).getTime();
        gravacao.deleteAnnotation(selectedID);
        selectedID = -1;
        hasTextChanged = false;
        toggleContent(false);
        toggleImage(false);

        Gravacao.AnnotationTime[] times = gravacao.getAnnotationTimes();
        List<Gravacao.AnnotationTime> lTimes = Arrays.stream(times)
                .filter(x -> x.time < time)
                .sorted()
                .collect(Collectors.toList());
        Gravacao.AnnotationTime a =
                lTimes.isEmpty() ?
                        new Gravacao.AnnotationTime(-1, 0) :
                        lTimes.get(lTimes.size() - 1);
        selectAnnotation(a.id);
    }

    public void toggleSelector(boolean toggle) {
        selectionPane.setVisibility(toggle ? View.VISIBLE : View.GONE);
    }

    public void toggleContent(boolean toggle) {
        contentPane.setVisibility(toggle ? View.VISIBLE : View.GONE);
    }

    public void toggleImage(boolean toggle) {
        imagePane.setVisibility(toggle ? View.VISIBLE : View.GONE);
    }

    public void saveOnClick(View view) {
        saveAnnotation();
        toggleContent(false);
        toggleImage(false);
        selectedID = -1;
    }

    public void deleteOnClick(View view) {
        if (selectedID == -1) return;

        Gravacao.Annotations a = gravacao.getAnnotation(selectedID);
        if (a.hasText() || a.hasImage()) // no reason to require confirmation for empty annotations
            new AlertDialog.Builder(getContext()).setMessage(
                    "Deseja mesmo remover esta anotação?")
                    .setPositiveButton("Remover", (d, i) -> deleteAnnotation())
                    .setNegativeButton("Cancelar", null)
                    .show();
        else deleteAnnotation();

        adapter.notifyDataSetChanged();
    }

    public void newButtonOnClick ( View view ) {
        if ( gravacao == null ) return;
        String name = "Anotacão " + (gravacao.getAnnotationCount() + 1);
        Gravacao.Annotations a = gravacao.addAnnotation(activityListener.getGravacaoTime(),
                name);
        selectAnnotation(a.id);
        adapter.notifyDataSetChanged();
    }

    public void takePictureOnClick ( View view ) {
        final int id = selectedID;
        activityListener.takePicture((Uri uri) -> {
            gravacao.setAnnotationImage(id, uri.toString());
            if (selectedID == id) // until image search returns, current annotation may change
                loadAnnotationImage();
        });
    }

    public void imageOnClick(View view) {
        imagePreview = !imagePreview;
        toggleContent(imagePreview);
        toggleSelector(imagePreview);
    }

    public interface PictureListener {
        void onPictureFound(Uri uri);
    }

    public interface AnnotationFragmentListener {
        Gravacao getGravacao ();

        int getGravacaoTime ();

        void receiveFragment ( AnnotationsFragment f );

        void onAnnotationChanged ( int ID, boolean first );

        void takePicture(PictureListener l);
    }
}
