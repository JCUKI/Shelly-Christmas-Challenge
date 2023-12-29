package com.example.indoor_positioning_app;

import static java.lang.Thread.sleep;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Algorithms {
    BeaconScanner _beaconScanner = null;
    MQTTHelper _mqttHelper = null;
    private int _waitingTime = 0;

    public int imageWidth, imageHeight, numberOfFloors, gridResolution;

    private volatile int[] calculatedXYZLocation;

    private ReentrantLock _lockObject;

    private Hashtable<String, Double[][][]> RSSIGrids = null;


    public Algorithms(BeaconScanner beaconScanner, MQTTHelper mqttHelper)
    {
        _beaconScanner = beaconScanner;
        _mqttHelper = mqttHelper;
        _waitingTime = 0;

        RSSIGrids = new Hashtable<>();

        _lockObject = new ReentrantLock();
    }

    public int[] getXYZLocation() {
        return calculatedXYZLocation;
    }

    // Helper function to calculate Euclidean distance in 3D
    private static double calculateDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2) + Math.pow((z2 - z1), 2));
    }

    //IDW implementation was generated using ChatGPT
    // Function to calculate inverse distance weighting interpolation in 3D
    public static double IDW(List<DataPoint3D> dataPoints, double x, double y, double z, int power) {
        double numerator = 0.0;
        double denominator = 0.0;

        for (DataPoint3D point : dataPoints) {
            double distance = calculateDistance3D(point.getX(), point.getY(), point.getZ(), x, y, z);

            if (distance == 0) {
                // Return the value directly if it's at a sample point
                return point.getValue();
            }

            double weight = 1.0 / Math.pow(distance, power);
            numerator += weight * point.getValue();
            denominator += weight;
        }

        if (denominator == 0) {
            // Avoid division by zero
            return 0.0;
        }
        return numerator / denominator;
    }

    //It starts to update RSSI grid as soon as it obtains any MQTT data
    public void FillRSSIGrid(String ShellyDevice)
    {
        List<DataPoint3D> dataPoints = GetDataPointsForDevice(ShellyDevice);

        if(dataPoints.size()<2)
        {
            return;
        }
        Double[][][] RSSIGrid = new Double[(int) (imageWidth / gridResolution)][(int) (imageHeight / gridResolution)][numberOfFloors];

        RSSIGrid =  UpdateRSSIGrid(RSSIGrid, gridResolution, dataPoints);
        RSSIGrids.put(ShellyDevice, RSSIGrid);
    }

    private Double[][][] UpdateRSSIGrid(Double[][][] grid, int resolution, List<DataPoint3D> dataPoints)
    {
        for (int z = 0; z < grid[0][0].length; z++)
        {
            for (int x = 0; x < grid.length; x++)
            {
                for (int y = 0; y < grid[0].length; y++)
                {
                    grid[x][y][z] = IDW(dataPoints,(x*resolution) + (int)(resolution/2),(y*resolution) + (int)(resolution/2),z,2);
                }
            }
        }
        return grid;
    }

    private List<DataPoint3D> GetDataPointsForDevice(String deviceName)
    {
        List<DataPoint3D> result = new ArrayList<DataPoint3D>();

        Enumeration<String> dictionaryKeys = _mqttHelper.mqttDataDict.keys();

        double deviceX=0, deviceY=0, deviceZ=0;

        while (dictionaryKeys.hasMoreElements())
        {
            String key = dictionaryKeys.nextElement();

            MQTTData data = _mqttHelper.mqttDataDict.get(key);

            if(data.SrcShellly().equals(deviceName))
            {
                deviceX = data.X();
                deviceY = data.Y();
                deviceZ = data.Z()*100;
            }
            else if(data.DetectedShellly().equals(deviceName))
            {
                result.add(new DataPoint3D(data.X(), data.Y(), data.Z()*100, data.RSSI()));
            }
        }
        //-30 is max signal, according to: https://www.metageek.com/training/resources/understanding-rssi/
        result.add(new DataPoint3D(deviceX, deviceY, deviceZ, -67));

        return result;
    }

    public int[] GetCurrentPositionWithIDW()
    {
        int[] currentClossest = new int[4];//xyz, distance
        currentClossest[2] = Integer.MIN_VALUE;
        currentClossest[3] = Integer.MAX_VALUE;

        Object[] shellyDevices = _mqttHelper.UniqueShellyList().toArray();


        for (int x = 0; x < RSSIGrids.get(shellyDevices[0]).length; x++) {
            for (int y = 0; y < RSSIGrids.get(shellyDevices[0])[x].length; y++) {
                for (int z = 0; z < RSSIGrids.get(shellyDevices[0])[x][z].length; z++) {
                    Enumeration<String> RSSIGridsKeys = RSSIGrids.keys();

                    int distance = 0;
                    while (RSSIGridsKeys.hasMoreElements()) {
                        String key = RSSIGridsKeys.nextElement();
                        int gridValue = Integer.valueOf(RSSIGrids.get(key)[x][y][z].intValue());
                        int RSSIValue = _beaconScanner.beaconDataDict.get(key)._rssi;//DETECTED VALUE OF SMARTPHONE
                        distance +=Math.abs(gridValue-RSSIValue);
                    }

                    if(distance < currentClossest[3])
                    {
                        currentClossest[3] = distance;
                        currentClossest[0] = x*gridResolution;
                        currentClossest[1] = y*gridResolution;
                        currentClossest[2] = z;
                    }
                }
            }
        }
        return currentClossest;
    }

    private Boolean IsMQTTDataAvailable()
    {
        Enumeration<String> dictionaryKeys = _mqttHelper.mqttDataDict.keys();

        if(_mqttHelper.mqttDataDict.size() == 0)
        {
            return false;
        }

        while (dictionaryKeys.hasMoreElements()) {
            String key = dictionaryKeys.nextElement();

            MQTTData data = _mqttHelper.mqttDataDict.get(key);

            String reverseKey = data.DetectedShellly() + "_" + data.SrcShellly();

            if(!_mqttHelper.mqttDataDict.containsKey(reverseKey))
            {
                return false;
            }
        }
        return true;
    }

    private Boolean IsBeaconScannerDataAvailable(int waitingTime)
    {
        Enumeration<String> beaconDictionaryKeys = _beaconScanner.beaconDataDict.keys();

        int foundEntries = 0;

        while (beaconDictionaryKeys.hasMoreElements())
        {
            String beaconKey = beaconDictionaryKeys.nextElement();

            for (String entry: _mqttHelper.UniqueShellyList())
            {
                if(beaconKey.equals(entry))
                {
                    foundEntries++;
                }
            }
        }

        //Tries to find data for all shellys in beaconDataDict,
        if(waitingTime < 10000)
        {
            return foundEntries == _mqttHelper.UniqueShellyList().size();
        }
        else// If it takes too long, try to find at least two entries
        {
            return foundEntries > 1;
        }
    }

    public Boolean IsDataAvailable(int waitingTime)
    {
        return IsMQTTDataAvailable() && IsBeaconScannerDataAvailable(waitingTime);
    }
}
