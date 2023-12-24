package com.example.indoor_positioning_app;

//This was taken from: https://github.com/hipstermartin/indoor-positioning-system/tree/main/Beacon-scanner
public interface RegionsBeaconService {
    void manualScan();
    void setListener(BeaconListener listener);
    boolean isBlueToothOn();
    void restart();
}