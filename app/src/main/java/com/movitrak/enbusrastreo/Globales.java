package com.movitrak.enbusrastreo;

public class Globales {
    public String servidor="http://cb.movitrak.net/";
    public String longitud="";
    public String latitud="";
    public String inversa="";
    public String normal="";
    public String status="NORMAL";
    public String chofer="";
    public String unidad="";

    public boolean compartiendo=false;

    private Globales() {}

    private static Globales _sharedInstance;
    public static Globales sharedInstance() {
        if(_sharedInstance == null)
            _sharedInstance = new Globales();
        return _sharedInstance;
    }


}
