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
        } catch (SecurityException secEx) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                if (_Nc.connectWithServer()) {
                    //GPS Request for location every 5 seconds
                    Log.d("Calculando Velocidad", "");
                    puntoB = _MobileLocation;
                    int speed = (int) fSpeed(puntoA, puntoB, (int) interval);
                    send = false;
                    if (puntoB.distanceTo(puntoA) > 3) {
                        send = true;
                        if (speed < 39) {
                            interval = 15;
                        } else {
                            if (speed < 80) {
                                interval = 10;
                            } else {
                                interval = 5;
                            }
                        }
                    }
                    while (send == true) {
                        //if (_Nc.chkWithServer())	{

                        _Nc.sendDataWithString(buildStringSocket(puntoB));

                        //_Nc.disConnectWithServer();
                        //_Nc.connectWithServer();
                    }
                    puntoA = puntoB;
                }
                Thread.sleep(interval * 1000);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                _Nc.disConnectWithServer();
                _Nc.connectWithServer();
            }

        }
    }

    private String buildStringSocket(Location punto) {
        StringBuilder socketStringBuilder = new StringBuilder();
        double lat = punto.getLatitude();
        double lon = punto.getLongitude();
        if (lat >= 0 && lon >= 0) {
            socketStringBuilder.append("0");
        } else if (lat >= 0 && lon < 0) {
            socketStringBuilder.append("1");
        } else if (lat < 0 && lon >= 0) {
            socketStringBuilder.append("2");
        } else if (lat < 0 && lon < 0) {
            socketStringBuilder.append("3");
        }

        DecimalFormat latFormat = new DecimalFormat("##.######");
        String latitude = latFormat.format(Math.abs(lat));
        latitude = latitude.replace(",", "");
        latitude = latitude.replace(".", "");
        if (Math.abs(lat) < 10) {
            socketStringBuilder.append("0");
        }
        socketStringBuilder.append(latitude);

        DecimalFormat lonFormat = new DecimalFormat("###.######");
        String longitude = lonFormat.format(Math.abs(lon));
        longitude = longitude.replace(",", "");
        longitude = longitude.replace(".", "");
        if (Math.abs(lon) < 10) {
            socketStringBuilder.append("00");
        } else if (Math.abs(lon) < 100) {
            socketStringBuilder.append("0");
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
        String deviceId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
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
