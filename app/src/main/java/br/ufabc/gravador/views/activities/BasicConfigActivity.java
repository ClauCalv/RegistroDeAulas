package br.ufabc.gravador.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.ufabc.gravador.R;

public class BasicConfigActivity extends AbstractMenuActivity {

    private static final List<ConfigItem> configOptions = new ArrayList<>(Arrays.asList(
            new ConfigItem("Cores", ConfigCoresActivity.class),
            new ConfigItem("Tamanho das Letras", ConfigFontsizeActivity.class),
            new ConfigItem("Negrito", ConfigBoldfontActivity.class),
            new ConfigItem("Alto Contraste", ConfigContrastActivity.class)
    ));
    RecyclerView configList;

    protected int getLayoutID() {
        return R.layout.activity_basic_config;
    }

    @Override
    protected void onSuperCreate(@Nullable Bundle savedInstanceState) {

        configList = findViewById(R.id.gravacaoList);
        configList.setLayoutManager(new LinearLayoutManager(this));
        configList.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new MyViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_config_card, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
                MyViewHolder myHolder = (MyViewHolder) holder;
                myHolder.v.setOnClickListener((view) -> selectConfig(position));
                myHolder.configName.setText(configOptions.get(position).name);
            }

            @Override
            public int getItemCount() {
                return configOptions.size();
            }

            class MyViewHolder extends RecyclerView.ViewHolder {
                View v;
                TextView configName;

                public MyViewHolder(View itemView) {
                    super(itemView);
                    v = itemView;
                    configName = v.findViewById(R.id.config_name);
                }
            }
        });
    }

    public void selectConfig(int position) {
        Intent intent = new Intent(this, configOptions.get(position).mClass);
        startActivity(intent);
    }

    private static class ConfigItem {
        final String name;
        final Class mClass;

        ConfigItem(String name, Class mClass) {
            this.name = name;
            this.mClass = mClass;
        }
    }

}
