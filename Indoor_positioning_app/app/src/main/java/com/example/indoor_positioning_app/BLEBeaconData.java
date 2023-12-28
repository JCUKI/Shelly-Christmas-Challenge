package com.example.indoor_positioning_app;

public class BLEBeaconData {
    double _distance;
    String _name;
    int _rssi;
    String _mAdress;


    public BLEBeaconData(double distance, String name, int rssi, String mAdress)
    {
        _distance = distance;
        _name = name;
        _rssi = rssi;
         _mAdress = mAdress;
    }
}
