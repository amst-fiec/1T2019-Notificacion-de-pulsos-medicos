package com.grupo2.iomt;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GraficoPulsosActivity extends AppCompatActivity {
    BarChart barChart;
    String token;
    String urlRegistoPulsos = "https://amstdb.herokuapp.com/db/registroDePulsos";
    String urlAmbulancia = "https://amstdb.herokuapp.com/db/ambulancia";
    String urlPulsos = "https://amstdb.herokuapp.com/db/pulsos";
    RequestQueue queue;
    TableLayout table;
    ArrayList<RegistroPulso> registroPulsos;
    ArrayList<Ambulancia> ambulancias;
    ArrayList<Pulso> pulsos;
    Map<String, String> params;
    Map<String, String> prioridades;
    Map<String, String> codseñales;
    boolean banderaActualizando = false;

    Handler handler;
    Runnable runnable;

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafico_pulsos);

        token = getIntent().getExtras().getString("token");
        queue = Volley.newRequestQueue(this);

        params = new HashMap<String, String>();
        params.put("Authorization", "JWT " + token);

        barChart = (BarChart)findViewById(R.id.barchart);
        table = (TableLayout) findViewById(R.id.table1);

        registroPulsos = new ArrayList<>();
        ambulancias = new ArrayList<>();
        pulsos = new ArrayList<>();

        prioridades = new HashMap<>();
        prioridades.put("Señal desconocida", "Baja");
        prioridades.put("Hiperpirexia", "Alta");
        prioridades.put("Presion arterial baja", "Media");
        prioridades.put("Arritmia", "Baja");
        prioridades.put("Paro cardiaco", "Alta");
        prioridades.put("Presion arterial alta", "Media");
        prioridades.put("Sin señal", "Baja");

        codseñales = new HashMap<>();
        codseñales.put("SED", "Desconocida");
        codseñales.put("HIP", "Hiperpirexia");
        codseñales.put("PAB", "Presion Arterial Baja");
        codseñales.put("ARR", "Arritmia");
        codseñales.put("PCA", "Paro Cardiaco");
        codseñales.put("PAA", "Presion Arterial Alta");

        init_Barchart(barChart);

            handler = new Handler();
       runnable = new Runnable() {
           @Override
           public void run() {
               registroPulsos = new ArrayList<>();
               ambulancias = new ArrayList<>();
               pulsos = new ArrayList<>();
               actualizar();
               handler.postDelayed(this, 10000);
           }
       };
       runnable.run();

    }

    @Override
    public void onBackPressed() {
        handler.removeCallbacks(runnable);

        super.onBackPressed();

    }
    public void irDetalles(View v){
        handler.removeCallbacks(runnable);
        Intent intent = new Intent(getApplicationContext(), Table_Registros_Pulsos_Activity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }

    public void actualizar(){
        System.out.println("Acccccccccc");
        obtenerRegistros();
        obtenerAmbulancias();
        obtenerPulsos();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                addAmbulaciaAndPulso(registroPulsos);
                Map<String, Integer> data = contarPulsos(registroPulsos);
                setDataBarchart(barChart, data);
                barChart.notifyDataSetChanged();
                barChart.invalidate();
                findViewById(R.id.btnDetalles).setVisibility(View.VISIBLE);

            }
        }, 2000);
    }

    public void setDataBarchart(BarChart barChart, Map<String, Integer> map){
            String[] labels = new String[map.keySet().size()];
            ArrayList<BarEntry> barEntries = new ArrayList<>();

            int maxValue = 0;
            Iterator <Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
            int counter = 0;
            while (iterator.hasNext()){
                Map.Entry<String, Integer> i = iterator.next();
                String key = i.getKey();
                Integer value = i.getValue();
                barEntries.add(new BarEntry(counter, value));

                labels[counter] = key;
                counter ++;
                if (value > maxValue)
                    maxValue = value;
            }
            BarDataSet barDataSet = new BarDataSet(barEntries, "Pulsos");
            barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);


            BarData barData = new BarData(barDataSet);
            //barData.setBarWidth(0.9f);

            barChart.setData(barData);
            add_labels_Barchart(barChart, labels);

    }

    public void add_labels_Barchart(BarChart barChart, String[] labels){
        IndexAxisValueFormatter indexFormatter = new IndexAxisValueFormatter();
        indexFormatter.setValues(labels);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(indexFormatter);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        xAxis.setCenterAxisLabels(true);
        xAxis.setLabelRotationAngle(45);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setGranularityEnabled(true);

        xAxis.setXOffset(-20);

    }
    public void init_Barchart(BarChart barChart){
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(false);
        //barChart.setMaxVisibleValueCount(50);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(true);
        barChart.getLegend().setEnabled(false);
        barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

    }

    public void obtenerRegistros(){
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlRegistoPulsos, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i<response.length(); i++){
                                JSONObject registro = response.getJSONObject(i);

                                String[] cadenaSplit = registro.getString("fecha_registro").split("T");
                                String fecha = cadenaSplit[0];
                                String hora = cadenaSplit[1].split("\\.")[0];

                                int pulso = registro.getInt("pulso");
                                int ambulancia = registro.getInt("ambulancia");
                                int id = registro.getInt("id");

                                RegistroPulso registroPulso = new RegistroPulso(id,fecha,hora,pulso,ambulancia);
                                registroPulsos.add(registroPulso);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return params;
            }
        };
        queue.add(request);
    }

    public void obtenerAmbulancias(){
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlAmbulancia , null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i<response.length(); i++){
                            try {
                                JSONObject ambulanciaJ = response.getJSONObject(i);
                                int id = ambulanciaJ.getInt("id");
                                String placa = ambulanciaJ.getString("placa");
                                boolean ocuapdo = ambulanciaJ.getBoolean("ocupado");
                                int conductor = ambulanciaJ.getInt("conductor");
                                Ambulancia ambulancia = new Ambulancia(id,placa, ocuapdo,conductor);
                                ambulancias.add(ambulancia);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Erooor respuesta");
                System.out.println(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return params;
            }
        };
        queue.add(request);
    }


    public void obtenerPulsos(){
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, urlPulsos, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        for(int i = 0 ; i < response.length(); i++){
                            try {
                                JSONObject pulsoJ = response.getJSONObject(i);
                                int id = pulsoJ.getInt("id");
                                String nombre = pulsoJ.getString("nombre");
                                if(codseñales.containsKey(nombre))
                                    nombre = codseñales.get(nombre);
                                int  numero_pulsos = pulsoJ.getInt("numero_pulsos");
                                String descripcion = pulsoJ.getString("descripcion");
                                Pulso pulso = new Pulso(id,nombre,numero_pulsos,descripcion);
                                pulsos.add(pulso);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Erooor respuesta");
                System.out.println(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return params;
            }
        };
        queue.add(request);
    }

    public Map<String, Integer> contarPulsos(ArrayList<RegistroPulso> registroPulsos){
        Map<String, Integer> contador = new HashMap<>();
        for (int i = 0; i<registroPulsos.size(); i++){
            RegistroPulso registroPulso = registroPulsos.get(i);
            Pulso pulso = registroPulso.getPulso();
            String nombre = pulso.nombre;
            try{
                Integer value = contador.get(nombre) + 1;
                //contador.remove(nombre);
                contador.put(nombre,value);
            }
            catch (Exception e){
                contador.put(nombre,1);
            }
        }
        return contador;
    }

    public Ambulancia getAmbulancia(ArrayList<Ambulancia> array, int id ){
            for (int i = 0; i<array.size(); i++){
                Ambulancia ambulancia= array.get(i);
                if(ambulancia.id == id)
                    return ambulancia;
            }
            return new Ambulancia();
    }
    public Pulso getPulso (ArrayList<Pulso> array, int id ){
        for (int i = 0; i<array.size(); i++){
            Pulso pulso= array.get(i);
            if(pulso.id == id)
                return pulso;
        }
        return new Pulso();
    }

    public void addAmbulaciaAndPulso(ArrayList<RegistroPulso> registroPulsos){
            for (int i = 0; i <registroPulsos.size(); i++){
                RegistroPulso registroPulso = registroPulsos.get(i);
                Pulso pulso =  getPulso(pulsos, registroPulso.pulsoID);
                Ambulancia ambulancia = getAmbulancia(ambulancias, registroPulso.ambulanciaID);
                registroPulso.setPulso(pulso);
                registroPulso.setAmbulancia(ambulancia);
            }
    }


}
