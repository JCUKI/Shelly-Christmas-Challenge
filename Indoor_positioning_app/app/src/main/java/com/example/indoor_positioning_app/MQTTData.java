package com.example.indoor_positioning_app;

import com.j256.ormlite.stmt.query.In;

public class MQTTData {

    private String _topic;
    private String _data;

    private String _detectedMac;

    private int _x,_y,_z;

    private String _detectedShellly, _srcShellly;
    private int _rssi;

    public int X()
    {
        return _x;
    }
    public int Y()
    {
        return _y;
    }
    public int Z()
    {
        return _z;
    }

    public int RSSI()
    {
        return _rssi;
    }

    public String DetectedMacMAC()
    {
        return _detectedMac.toUpperCase();
    }

    public String DetectedShellly()
    {
        return _detectedShellly.toUpperCase();
    }

    public String SrcShellly()
    {
        return _srcShellly.toUpperCase();
    }

    private int TryParseInt(String value, int defaultVal) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public MQTTData()
    {

    }

    private void ParseData(String shellyData)
    {
        String[] splitData = shellyData.split("\\##",0);

        _x = Integer.parseInt(splitData[1]);
        _y = Integer.parseInt(splitData[2]);
        _z = Integer.parseInt(splitData[3]);

        _detectedShellly = splitData[4];

        int index = _detectedShellly.indexOf("-");

        _detectedMac = splitData[0].replace(":", "");;

        _rssi = TryParseInt(splitData[5], 0);

        _srcShellly = splitData[6];
    }

    public void SetData(String shellyData)
    {
        _data = shellyData;
        ParseData(shellyData);
    }
}
