package com.example.indoor_positioning_app;

public interface BeaconListener {
    void beaconRecieved(String uuid, int minor, int mayor,double distance,String name,int rssi, String mAdress);
    void scaning(Boolean scanning);
}