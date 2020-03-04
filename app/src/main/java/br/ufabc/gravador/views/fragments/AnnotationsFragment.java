package br.ufabc.gravador.views.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
    private boolean hasTextChanged = false, textEmpty = true;

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
                    context.toString() + " must implement OnFragmentInteractionListener");
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
                selectNewAnnotation(l);
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
        annotationContent.setEnabled(false);

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
        annotationNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                newButtonOnClick();
            }
        });

        annotationTakePicture = master.findViewById(R.id.annotationTakePicture);
        annotationTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                takePictureOnClick();
            }
        });

        annotationSave = master.findViewById(R.id.annotationSave);
        annotationSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                saveOnClick();
            }
        });

        annotationDelete = master.findViewById(R.id.annotationDelete);
        annotationDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick ( View view ) {
                deleteOnClick();
            }
        });

        annotationTime = master.findViewById(R.id.annotationTime);

        gravacao = activityListener.getGravacao();
        activityListener.receiveFragment(this);

        loadNewGravacao();
        adapter.notifyDataSetChanged();
        hasTextChanged = false;
    }

    public void loadNewGravacao () {
        annotationName.setText("Crie uma anotação");
        annotationTime.setText("00:00");
    }

    public interface annotationSavedListener {
        void onAnnotationSaved ();
    }

    public void alertSave ( annotationSavedListener listener ) {
        if ( hasTextChanged && selectedID != -1 )
            new AlertDialog.Builder(getContext()).setMessage(
                    "Salvar mudanças na anotação anterior?")
                    .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            if ( i == AlertDialog.BUTTON_POSITIVE ) saveOnClick();
                            listener.onAnnotationSaved();
                        }
                    })
                    .setNegativeButton("Descartar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            listener.onAnnotationSaved();
                        }
                    })
                    .show();
        else listener.onAnnotationSaved();
    }

    public void jumpToTime ( int millisec ) {
        int ID = gravacao.getAnnotationIDOnTime(millisec);
        if ( hasTextChanged && ID != -1 )
            new AlertDialog.Builder(getContext())
                    .setMessage("Salvar mudanças na anotação anterior?")
                    .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick ( DialogInterface dialogInterface, int i ) {
                            if ( i == AlertDialog.BUTTON_POSITIVE ) saveOnClick();
                            changeAnnotation(ID);
                        }
                    }).setNegativeButton("Descartar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick ( DialogInterface dialogInterface, int i ) {
                    changeAnnotation(ID);
                }
            }).show();
        else changeAnnotation(ID);
    }

    public void changeAnnotation ( int ID ) {
        if ( ID == -1 ) return;
        selectedID = ID;
        Gravacao.Annotations a = gravacao.getAnnotation(selectedID);
        annotationName.setText(a.getName());
        annotationTime.setText(a.getTimeStamp());
        annotationContent.setEnabled(true);
        annotationContent.setText(a.getText());
        hasTextChanged = false;
        adapter.notifyDataSetChanged();
        activityListener.onAnnotationChanged(ID, true);
        if ( a.hasImage() ) {
            //TODO images
        }
    }

    public void selectNewAnnotation ( long ID ) {
        boolean firstTime = selectedID == -1;
        if ( ID == -1 || ID == selectedID ) return;

        changeAnnotation((int) ID);
        activityListener.onAnnotationChanged(selectedID, firstTime);
    }

    public void saveOnClick () {
        gravacao.setAnnotationName(selectedID, annotationName.getText().toString());
        gravacao.setAnnotationText(selectedID, annotationContent.getText().toString());
        adapter.notifyDataSetChanged();
        hasTextChanged = false;
    }

    public void deleteOnClick () {
        final int time = gravacao.getAnnotation(selectedID).getTime();
        gravacao.deleteAnnotation(selectedID);

        int[] times = gravacao.getAnnotationTimes();

        List<Integer> lTimes = Arrays.stream(times)
                .filter(x -> x < time)
                .sorted()
                .boxed()
                .collect(Collectors.toList());

        hasTextChanged = false;
        jumpToTime(lTimes.isEmpty() ? 0 : lTimes.get(lTimes.size() - 1));
    }

    public void newButtonOnClick () {
        alertSave(new annotationSavedListener() {
            @Override
            public void onAnnotationSaved () {
                String name = "Anotacão " + ( gravacao.getAnnotationCount() + 1 );
                Gravacao.Annotations a = gravacao.addAnnotation(activityListener.getGravacaoTime(),
                        name);
                selectNewAnnotation(a.id);
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void takePictureOnClick () {
        //TODO
    }

    public interface AnnotationFragmentListener {
        Gravacao getGravacao ();

        int getGravacaoTime ();

        void receiveFragment ( AnnotationsFragment f );

        void onAnnotationChanged ( int ID, boolean first );
    }
}
