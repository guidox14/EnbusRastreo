package com.movitrak.enbusrastreo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.Console;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Adrian on 23/01/2018.
 */

public class Rastreo extends IntentService implements LocationListener {

    Location _MobileLocation;
    LocationManager _LocManager;
    NetClient _Nc;

    private final String netClientIP = "201.206.34.30"; //"192.168.1.15"; //"201.206.34.30"
    private final int port = 4003;

    private final int minDistance = 3;

    public Rastreo() {
        super("Rastreo");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d("Enter onHandleIntent", "Initializing Rastreo.class");
        System.out.println("Enter onHandleIntent");
        _LocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        long interval = 5; // Seconds
        try {
            _LocManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 0, this, Looper.getMainLooper());
        }
        catch (SecurityException secEx) {
            secEx.printStackTrace();
            String message = secEx.getMessage();
            System.out.println(message);
        }
        Location puntoA = null, puntoB = null;
        boolean send = false;
        _Nc = new NetClient(netClientIP, port);
        //_Nc = new NetClient("201.203.34.40", 4003);
        try {
            while (puntoA == null) {
                puntoA = _MobileLocation;
                Log.d("Punto A", "Punto A sigue siendo nulo");
                Thread.sleep(500);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (_Nc.connectWithServer()) {
                while (true) {
                    //GPS Request for location every 5 seconds
                    Log.d("Calculando Velocidad", "");
                    puntoB = _MobileLocation;
                    int speed = (int) fSpeed(puntoA, puntoB, (int) interval);
                    if (puntoB.distanceTo(puntoA) > minDistance) {
                        Log.d("Debug", "La distancia es mayor a: " + minDistance);
                        if (speed < 39) {
                            interval = 15;
                        }
                        else {
                            if (speed < 80) {
                                interval = 10;
                            }
                            else {
                                interval = 5;
                            }
                        }
                        String socketString = buildStringSocket(puntoB);
                        _Nc.sendDataWithString(socketString);
                        Log.d("Debug", "Enviando socket string: " + socketString);

                        //_Nc.disConnectWithServer();
                        //_Nc.connectWithServer();
                        puntoA = puntoB;
                    }
                }
            }
            else {
                Thread.sleep(interval * 1000);
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            _Nc.disConnectWithServer();
            _Nc.connectWithServer();
        }

    }

    /*
     * Recibe un string con la ubicacion, y completa los decimales faltantes con 0s
     */
    private String CompleteSpaces(String location, int totalSpaces) {
        if(location.length() < totalSpaces) {
            int newTotalSpaces = totalSpaces - location.length();
            for(int i = 0; i < newTotalSpaces; i++) {
                location = location + "0";
            }
        }
        return location;
    }

    private String buildStringSocket(Location punto) {
        StringBuilder socketStringBuilder = new StringBuilder();
        double lat = punto.getLatitude();
        double lon = punto.getLongitude();
        if (lat >= 0 && lon >= 0) {
            socketStringBuilder.append("0");
        }
        else if (lat >= 0 && lon < 0) {
            socketStringBuilder.append("1");
        }
        else if (lat < 0 && lon >= 0) {
            socketStringBuilder.append("2");
        }
        else if (lat < 0 && lon < 0) {
            socketStringBuilder.append("3");
        }

        DecimalFormat latFormat = new DecimalFormat("##.######");
        String latitude = latFormat.format(Math.abs(lat));
        latitude = latitude.replace(",", "");
        latitude = latitude.replace(".", "");
        if (Math.abs(lat) < 10) {
            socketStringBuilder.append("0");
            // Llama a CompleteSpaces
            latitude = CompleteSpaces(latitude, 7);
        }
        else {
            // Llama a CompleteSpaces
            latitude = CompleteSpaces(latitude, 6);
        }
        socketStringBuilder.append(latitude);

        DecimalFormat lonFormat = new DecimalFormat("###.######");
        String longitude = lonFormat.format(Math.abs(lon));
        longitude = longitude.replace(",", "");
        longitude = longitude.replace(".", "");
        if (Math.abs(lon) < 10) {
            socketStringBuilder.append("00");
            // Llama a CompleteSpaces
            longitude = CompleteSpaces(longitude, 7);
        } else if (Math.abs(lon) < 100) {
            socketStringBuilder.append("0");
            // Llama a CompleteSpaces
            longitude = CompleteSpaces(longitude, 8);
        }
        socketStringBuilder.append(longitude);

        Date fechaActual = new Date();
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fechaActualString = simpleFormat.format(fechaActual);

        socketStringBuilder.append(fechaActualString);

        socketStringBuilder.append(IDDevice());

        return socketStringBuilder.toString();
    }

    //gets and return a Device ID
    private String IDDevice() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    //return a device speed between two locations in Kph
    private double fSpeed(Location loc1, Location loc2, int pTime) {
        double distance;
        distance = loc1.distanceTo(loc2);
        distance = distance / pTime;
        distance = distance * 3.6; // 3.6??????
        return distance;
    }

    @Override
    public void onLocationChanged(Location location) {
        _MobileLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
