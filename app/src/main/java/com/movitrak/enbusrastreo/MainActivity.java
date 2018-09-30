package com.movitrak.enbusrastreo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends Activity implements LocationListener, SensorEventListener {
    Context mContext = this;
    String ruta;
    String ruta_inversa;
    String numero;
    String longitud;
    String latitud;
    String testmsg;

    private JSONArray json;
    private JSONObject json_transferencia;
    private HTTPURLConnection service;
    int tamano, swresp;
    int progresstatus;
    private ProgressDialog pDialog;
    private ProgressDialog pDialogTransferir;
    ArrayList<String> id_rutas = new ArrayList<String>();
    ArrayList<String> items = new ArrayList<String>();
    ArrayList<String> inversas = new ArrayList<String>();
    ArrayList<String> id_choferes = new ArrayList<String>();
    ArrayList<String> items_choferes = new ArrayList<String>();
    ArrayList<String> id_unidades = new ArrayList<String>();
    ArrayList<String> items_unidades = new ArrayList<String>();
    ArrayAdapter<String> dataAdapter_unidad;
    ArrayAdapter<String> dataAdapter;
    ArrayAdapter<String> dataAdapter_chofer;
    Button btn_transferir, iviniciar;
    private Spinner rutas;
    private Spinner choferes;
    private Spinner unidades;
    LocationManager handle;
    private String provider;

    final Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        public void run() {
            enviar();
            handler.postDelayed(runnable, 5000);
        }
    };

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }


    final Handler handler2 = new Handler();
    Runnable runnable2 = new Runnable() {
        public void run() {
            descargar();
            handler2.postDelayed(runnable2, 21600000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        setContentView(R.layout.activity_main);
        iviniciar = findViewById(R.id.iviniciar);
        btn_transferir = findViewById(R.id.btn_transferir);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Dialog to ask for gps permission
            return;
        }
        handle = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        provider = handle.getBestProvider(c, true);
        handle.requestLocationUpdates(provider, 10000, 10, this);
        Location loc = handle.getLastKnownLocation(provider);
        muestraPosicionActual(loc);
        service = new HTTPURLConnection();
        new CargarSpinners().execute();

        unidades = findViewById(R.id.txtnumero);
        unidades.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Globales.sharedInstance().unidad = id_unidades.get(position);
                        numero = items_unidades.get(position);
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                        // showToast("Spinner1: unselected");
                    }
                });

        choferes = findViewById(R.id.chofer);
        choferes.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Globales.sharedInstance().chofer = id_choferes.get(position);
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                        // showToast("Spinner1: unselected");
                    }
                });

        rutas = findViewById(R.id.spruta);
        rutas.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ruta = id_rutas.get(position);
                        ruta_inversa = inversas.get(position);
                        Globales.sharedInstance().normal = ruta;
                        Globales.sharedInstance().inversa = ruta_inversa;
                    }
                    public void onNothingSelected(AdapterView<?> parent) {
                        // showToast("Spinner1: unselected");
                    }
                });
    }

    public void onClickIniciarViaje(View v) {
        if (Globales.sharedInstance().unidad.equals("0") || Globales.sharedInstance().normal.equals("0") || Globales.sharedInstance().chofer.equals("0")) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Parametros incompletos")
                    .setMessage("Seleccionar todos los parametros!")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
        else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("unidad", Globales.sharedInstance().unidad);
            editor.putString("chofer", Globales.sharedInstance().chofer);
            editor.putString("ruta", Globales.sharedInstance().normal);
            editor.apply();
            Globales.sharedInstance().compartiendo = true;
            //handler.post(runnable);


            Intent rastreoIntent = new Intent(this, Rastreo.class);
            startService(rastreoIntent);

            this.finish();
            //txtstatus.setText("Compartiendo ubicación.");
                                                 /*Intent intent = new Intent(MainActivity.this, inicio.class);
                                                 intent.putExtra("latitud", latitud);
                                                 intent.putExtra("longitud", longitud);
                                                 intent.putExtra("unidad", numero);
                                                 if (Globales.sharedInstance().status == "NORMAL") {
                                                     intent.putExtra("ruta", Globales.sharedInstance().normal);
                                                 } else {
                                                     intent.putExtra("ruta", Globales.sharedInstance().inversa);
                                                 }
                                                 startActivity(intent);*/
        }
    }

    public void enviar() {
        new enviar_posicion().execute();
    }

    public void descargar() {
        //new save_passenger().execute();
    }

    protected class CargarSpinners extends AsyncTask<Void, Void, Void> {
        String response = "";
        String response2 = "";
        String response3 = "";
        //Create hashmap Object to send parameters to web service
        HashMap<String, String> postDataParams;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Cargando...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            postDataParams = new HashMap<String, String>();
            postDataParams.put("mobile", "true");
            //Call ServerData() method to call webservice and store result in response
            Log.i("variable", postDataParams.toString());
            response = service.ServerData(Globales.sharedInstance().servidor + "main.php?ACTION=mod_ws&OPERACION=show_rutas", postDataParams);
            response2 = service.ServerData(Globales.sharedInstance().servidor + "main.php?ACTION=mod_ws&OPERACION=show_choferes", postDataParams);
            response3 = service.ServerData(Globales.sharedInstance().servidor + "main.php?ACTION=mod_ws&OPERACION=show_unidades", postDataParams);
            try {
                json = new JSONArray(response);
                items.add("Seleccionar Ruta...");
                id_rutas.add("0");
                inversas.add("0");
                for (int i = 0; i < json.length(); i++) {
                    JSONObject obj = json.getJSONObject(i);
                    String id_ruta = obj.getString("id");
                    String nombre = obj.getString("descripcion");
                    String inverso = obj.getString("inversa");
                    items.add(nombre);
                    id_rutas.add(id_ruta);
                    inversas.add(inverso);
                }
                json = new JSONArray(response2);
                items_choferes.add("Seleccionar Chofer...");
                id_choferes.add("0");
                for (int i = 0; i < json.length(); i++) {
                    JSONObject obj = json.getJSONObject(i);
                    String id_chofer = obj.getString("id");
                    String nombre_chofer = obj.getString("nombre");
                    items_choferes.add(nombre_chofer);
                    id_choferes.add(id_chofer);
                }
                json = new JSONArray(response3);
                items_unidades.add("Seleccionar Unidad...");
                id_unidades.add("0");
                for (int i = 0; i < json.length(); i++) {
                    JSONObject obj = json.getJSONObject(i);
                    String id_unidad = obj.getString("id");
                    String nombre_unidad = obj.getString("nombre");
                    items_unidades.add(nombre_unidad);
                    id_unidades.add(id_unidad);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            Globales.sharedInstance().chofer = sharedPref.getString("chofer", null);
            Globales.sharedInstance().unidad = sharedPref.getString("unidad", null);
            Globales.sharedInstance().normal = sharedPref.getString("ruta", null);

            dataAdapter = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_item, items);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            rutas.setAdapter(dataAdapter);
            rutas.setSelection(id_rutas.indexOf(Globales.sharedInstance().normal));

            dataAdapter_chofer = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_item, items_choferes);
            dataAdapter_chofer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            choferes.setAdapter(dataAdapter_chofer);
            choferes.setSelection(id_choferes.indexOf(Globales.sharedInstance().chofer));

            dataAdapter_unidad = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_item, items_unidades);
            dataAdapter_unidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            unidades.setAdapter(dataAdapter_unidad);
            unidades.setSelection(id_unidades.indexOf(Globales.sharedInstance().unidad));

            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
        }
    }

    public void muestraPosicionActual(Location loc) {
        String coordenada = "";
        if (loc == null) {//Si no se encuentra localización, se mostrará "Desconocida"
            //longitud="Desconocida";
            //latitud="Desconocida";
            coordenada = "Desconocida";

        } else {//Si se encuentra, se mostrará la latitud y longitud
            //latitud=String.valueOf(loc.getLatitude());
            //latitud=String.valueOf(loc.getLongitude());
            coordenada = String.valueOf(loc.getLatitude()) + String.valueOf(loc.getLongitude());
            latitud = String.valueOf(loc.getLatitude());
            Globales.sharedInstance().latitud = String.valueOf(loc.getLatitude());
            longitud = String.valueOf(loc.getLongitude());
            Globales.sharedInstance().longitud = String.valueOf(loc.getLongitude());
            //Log.i("Lat",String.valueOf(loc.getLatitude()));
            //Log.i("Long",String.valueOf(loc.getLongitude()));
        }
        // message.setText(coordenada);
    }


    @Override
    public void onLocationChanged(Location location) {
        // Se ha encontrado una nueva localización
        muestraPosicionActual(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Proveedor deshabilitado
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Proveedor habilitado
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Ha cambiado el estado del proveedor
    }

    /*protected class save_passenger extends AsyncTask<Void, Integer, Void> {
        String response = "";
        //Create hashmap Object to send parameters to web service
        HashMap<String, String> postDataParams;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            DBHelper MDB = new DBHelper(getApplicationContext());
            MDB.open();
            ArrayList<passenger> dist = (ArrayList<passenger>) MDB.getPassenger();
            MDB.close();
            pDialogTransferir = new ProgressDialog(MainActivity.this);
            pDialogTransferir.setCancelable(false);
            String msg = "Subiendo: " + String.valueOf(dist.size()) + " Lecturas";
            SpannableString ss2=  new SpannableString(msg);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            //pDialogTransferir.setMessage("Subiendo: " + String.valueOf(dist.size()) + " Lecturas");
            pDialogTransferir.setMessage(ss2);
            pDialogTransferir.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            swresp = 0;
            postDataParams = new HashMap<String, String>();
            DBHelper MDB = new DBHelper(getApplicationContext());
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("MissingPermission") String IMEI_Number_Holder = telephonyManager.getDeviceId();
            MDB.open();
            ArrayList<passenger> dist = (ArrayList<passenger>) MDB.getPassenger();
            Iterator<passenger> nombreIterator = dist.iterator();
            tamano=dist.size();
            while(nombreIterator.hasNext()){
                passenger elemento = nombreIterator.next();
                postDataParams.put("fecha",elemento.getFecha());
                postDataParams.put("info_ws", elemento.getTipo());
                postDataParams.put("foto", elemento.getFoto());
                postDataParams.put("unidad", elemento.getUnidad());
                postDataParams.put("latitud", elemento.getLatitud());
                postDataParams.put("longitud", elemento.getLongitud());
                postDataParams.put("imei", IMEI_Number_Holder);
                postDataParams.put("chofer", Globales.sharedInstance().chofer);
                postDataParams.put("ruta", Globales.sharedInstance().normal);
                //Call ServerData() method to call webservice and store result in response
                Log.i("variable", postDataParams.toString());
                response= service.ServerData(Globales.sharedInstance().servidor+"main.php?ACTION=mod_ws&OPERACION=save_passenger",postDataParams);
                try {
                    json_transferencia = new JSONObject(response);
                    String nombre=json_transferencia.getString("message");
                    if(nombre.equals("1")){
                        MDB.execute("DELETE FROM passenger where id = "+elemento.getId());
                    }else{
                        swresp = 1;
                    }
                } catch (JSONException e) {
                    swresp = 1;
                }
                tamano--;
                publishProgress(tamano);
            }
            if(swresp == 0){
                MDB.execute("VACUUM");
            }
            MDB.close();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... tamano) {
            super.onProgressUpdate(tamano);
            String msg = "Subiendo: " + tamano[0] + " Lecturas";
            SpannableString ss2=  new SpannableString(msg);
            ss2.setSpan(new RelativeSizeSpan(2f), 0, ss2.length(), 0);
            ss2.setSpan(new ForegroundColorSpan(Color.BLACK), 0, ss2.length(), 0);
            //pDialogTransferir.setMessage("Subiendo: " + String.valueOf(dist.size()) + " Lecturas");
            pDialogTransferir.setMessage("Subiendo: " + tamano[0] + " Lecturas");
            pDialogTransferir.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (pDialogTransferir.isShowing()) {
                pDialogTransferir.dismiss();
                if(swresp == 0){
                    testmsg = "La transferencia de datos se ha realizado con exito";
                }else{
                    testmsg = "No se pudo trasnferir todos los datos. Reintente mas tarde.";
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Tranferencia Terminada")
                        .setMessage(testmsg)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
            //Intent intent = new Intent(aceptado.this, exito.class);
            //startActivity(intent);
            //finish();
            //dataAdapter = new ArrayAdapter<String>(MainActivity.this,
            //        android.R.layout.simple_spinner_item, items);
            //dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //rutas.setAdapter(dataAdapter);
        }
    }*/

    protected class enviar_posicion extends AsyncTask<Void, Void, Void> {
        String response = "";
        //Create hashmap Object to send parameters to web service
        HashMap<String, String> postDataParams;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //pDialog = new ProgressDialog(MainActivity.this);
            //pDialog.setMessage("Cargando...");
            //pDialog.setCancelable(false);
            //pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            postDataParams=new HashMap<String, String>();
            postDataParams.put("mobile", "true");
            //Call ServerData() method to call webservice and store result in response

            //Log.i("variable", Globales.sharedInstance().servidor+"main.php?ACTION=mod_ws&OPERACION=save_position&nombre="+numero+"&ruta="+ruta+"&longitud="+longitud+"&latitud="+latitud+"&grados="+mCurrentDegree+"&status=1");
            if (Globales.sharedInstance().status=="NORMAL") {
                //intent.putExtra("ruta", Globales.sharedInstance().normal);
                ruta=Globales.sharedInstance().normal;
            }
            else
            {
                //intent.putExtra("ruta", Globales.sharedInstance().inversa);
                ruta=Globales.sharedInstance().inversa;
            }
            Log.i("compartiendo",String.valueOf(Globales.sharedInstance().compartiendo));
            if(Globales.sharedInstance().compartiendo) {
                response = service.ServerData(Globales.sharedInstance().servidor + "main.php?ACTION=mod_ws&OPERACION=save_position&nombre=" + numero + "&ruta=" + ruta + "&longitud=" + longitud + "&latitud=" + latitud + "&grados=" + mCurrentDegree + "&status=1", postDataParams);
            }else{
                response = service.ServerData(Globales.sharedInstance().servidor + "main.php?ACTION=mod_ws&OPERACION=save_position&nombre=" + numero + "&ruta=" + ruta + "&longitud=" + longitud + "&latitud=" + latitud + "&grados=" + mCurrentDegree + "&status=0", postDataParams);
                handler.removeCallbacks(runnable);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }

            //dataAdapter = new ArrayAdapter<String>(MainActivity.this,
            //       android.R.layout.simple_spinner_item, items);
            //dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            //rutas.setAdapter(dataAdapter);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
            //Log.d("Ejemplo3", Float.toString(mOrientation[0]));
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
            //Log.d("Ejemplo4", Float.toString(mOrientation[0]));
        }

        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;

            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            //ra.setDuration(250);

            //ra.setFillAfter(true);

            //mPointer.startAnimation(ra);
            mCurrentDegree = -azimuthInDegress;
            //Toast.makeText(this, String.valueOf(mCurrentDegree), Toast.LENGTH_SHORT).show();
        }
    }
}
