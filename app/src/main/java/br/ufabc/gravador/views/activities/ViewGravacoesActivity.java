package br.ufabc.gravador.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import br.ufabc.gravador.R;
import br.ufabc.gravador.controls.helpers.MyFileManager;
import br.ufabc.gravador.models.Gravacao;

public class ViewGravacoesActivity extends AbstractServiceActivity {

    public static String loading = "Recuperando Gravações", loaded = "Gravações existentes:";
    private MyFileManager fileManager;
    private TextView txtGravacaoList;
    private RecyclerView gravacaoList;

    private List<Gravacao> gravacaos;

    private Handler loadHandler = null;
    private Runnable loadRunnable = new Runnable() {
        @Override
        public void run () {
            loadFiles();
            finishLoad();
        }
    };

    @SuppressLint( "MissingSuperCall" )
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState, R.layout.activity_view_recordings, R.id.my_toolbar,
                true);

        fileManager = MyFileManager.getInstance();
        fileManager.setup(getApplicationContext());

        txtGravacaoList = findViewById(R.id.txtGravacaoList);
        txtGravacaoList.setText(loading);

        gravacaoList = findViewById(R.id.gravacaoList);
        gravacaoList.setLayoutManager(new LinearLayoutManager(this));
        gravacaoList.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder ( @NonNull ViewGroup parent, int viewType ) {
                return new MyViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_gravacao_card, parent, false));
            }

            @Override
            public void onBindViewHolder ( @NonNull RecyclerView.ViewHolder holder, final int position ) {
                MyViewHolder myHolder = (MyViewHolder) holder;
                myHolder.v.setOnClickListener(( view ) -> selectGravacao(position));
                myHolder.gravacaoName.setText(gravacaos.get(position).getName());
            }

            @Override
            public int getItemCount () { return gravacaos == null ? 0 : gravacaos.size(); }

            class MyViewHolder extends RecyclerView.ViewHolder {
                View v;
                TextView gravacaoName;

                public MyViewHolder ( View itemView ) {
                    super(itemView);
                    v = itemView;
                    gravacaoName = v.findViewById(R.id.gravacaoName);
                }
            }
        });

    }

    @Override
    protected void onServiceOnline () {
        loadHandler = new Handler();
        loadHandler.postDelayed(loadRunnable, 0);
    }

    @Override
    protected void onResume () {
        super.onResume();
        if ( fileManager != null ) loadFiles();
        if ( gravacaoList != null ) gravacaoList.getAdapter().notifyDataSetChanged();
    }

    public void selectGravacao ( int position ) {
        Gravacao g = gravacaos.get(position);
        gravacaoService.setGravacao(g);
        Log.i("SELECTED", position + ": " + g.getName());
        Intent intent = new Intent(this, OpenGravacaoActivity.class);
        startActivity(intent);
    }

    public void loadFiles () {
        List<File> files = fileManager.listFiles(MyFileManager.GRAVACAO_DIR, new FilenameFilter() {
            @Override
            public boolean accept ( File file, String s ) {
                return s.endsWith(Gravacao.annotationExtension);
            }
        });
        gravacaos = new ArrayList<Gravacao>();
        for ( File f : files ) {
            Gravacao g = Gravacao.LoadFromFile(f.getParent(),
                    f.getName().split(Gravacao.annotationExtension)[0]);
            if ( g == null ) continue;
            gravacaos.add(g);
        }
    }

    public void finishLoad () {
        gravacaoList.getAdapter().notifyDataSetChanged();
        txtGravacaoList.setText(loaded);
    }
}
