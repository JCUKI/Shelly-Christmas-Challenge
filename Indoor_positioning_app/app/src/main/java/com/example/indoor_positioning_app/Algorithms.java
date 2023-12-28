package com.example.indoor_positioning_app;


import android.util.Log;

import com.j256.ormlite.stmt.query.In;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.XMLFormatter;

//IDW was generated using ChatGPT
public class Algorithms {
    BeaconScanner _beaconScanner = null;
    MQTTHelper _mqttHelper = null;

    private int _waitingTime = 0;

    public Algorithms(BeaconScanner beaconScanner, MQTTHelper mqttHelper)
    {
        _beaconScanner = beaconScanner;
        _mqttHelper = mqttHelper;
    }
    // Function to calculate inverse distance weighting interpolation in 3D
    public static double inverseDistanceWeighting3D(List<DataPoint3D> dataPoints, double x, double y, double z, int power) {
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

    // Helper function to calculate Euclidean distance in 3D
    private static double calculateDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2) + Math.pow((z2 - z1), 2));
    }

    // Sample data point class representing 3D coordinates and values
    static class DataPoint3D {
        private final double x;
        private final double y;
        private final double z;
        private final double value;

        public DataPoint3D(double x, double y, double z, double value) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.value = value;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getValue() {
            return value;
        }
    }
    public void DemoIDW()
    {
        // Sample data points in 3D
        List<DataPoint3D> dataPoints = new ArrayList<>();
        dataPoints.add(new DataPoint3D(1.0, 1.0, 1.0, 2.0));
        dataPoints.add(new DataPoint3D(2.0, 2.0, 2.0, 3.0));
        dataPoints.add(new DataPoint3D(3.0, 3.0, 3.0, 5.0));
        // Add more data points as needed

        // Point for which interpolation is needed in 3D
        double xInterpolated = 2.5;
        double yInterpolated = 2.5;
        double zInterpolated = 2.5;

        // Interpolate the value at the specified point using inverse distance weighting in 3D
        int power = 2; // Power parameter for inverse distance weighting
        double interpolatedValue = inverseDistanceWeighting3D(dataPoints, xInterpolated, yInterpolated, zInterpolated, power);

        System.out.println("Interpolated value at (" + xInterpolated + ", " + yInterpolated + ", " + zInterpolated + "): " + interpolatedValue);
    }

    private Double[][][] CreateRSSIGrid(int width, int height, int resolution, int numberOfFloors, List<DataPoint3D> dataPoints)
    {
        Double[][][] RSSIGrid = new Double[(int)(width/resolution)][(int)(height/resolution)][numberOfFloors];

        for (int z = 0; z < numberOfFloors; z++)
        {
            for (int x = 0; x < (int)(width/resolution); x++)
            {
                for (int y = 0; y < (int)(height/resolution); y++)
                {
                    RSSIGrid[x][y][z] = inverseDistanceWeighting3D(dataPoints,x,y,z,2);
                }
            }
        }
        return RSSIGrid;
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
                deviceZ = data.Z();
            }
            else if(data.DetectedShellly().equals(deviceName))
            {
                result.add(new DataPoint3D(data.X(), data.Y(), data.Z(), data.RSSI()));
            }
        }
        //-30 is max signal, according to: https://www.metageek.com/training/resources/understanding-rssi/
        result.add(new DataPoint3D(deviceX, deviceY, deviceZ, -30));

        return result;
    }

    private int[] GetXYZLocation(List<Double[][][]> RSSIGrids)
    {
        int[] XYZLocation = new int[3];

        int[] currentCLossest = new int[4];//xyz, distance
        currentCLossest[3] = Integer.MAX_VALUE;

        Object[] shellyDevices = _mqttHelper.UniqueShellyList.toArray();

        for (int z = 0; z < RSSIGrids.get(0)[0][0].length; z++) {
            for (int x = 0; x < RSSIGrids.get(0).length; x++) {
                for (int y = 0; y < RSSIGrids.get(0)[x].length; y++) {

                    int distance = 0;
                    for (int i = 0; i < RSSIGrids.size(); i++) {
                        int gridValue = Integer.valueOf(RSSIGrids.get(i)[x][y][z].intValue());
                        String ShellyDevice = shellyDevices[i].toString();
                        int RSSIValue = _beaconScanner.beaconDataDict.get(ShellyDevice)._rssi;//DETECTED VALUE OF SMARTPHONE
                        distance +=Math.abs(gridValue-RSSIValue);
                    }

                    if(distance < currentCLossest[3])
                    {
                        currentCLossest[3] = distance;
                        currentCLossest[0] = x;
                        currentCLossest[1] = x;
                        currentCLossest[2] = x;
                    }
                }
            }
        }
        return XYZLocation;
    }

    public int[] GetPositionWithIDW(int width, int height, int numberOfFloors, int resolution)
    {
        //https://jenkov.com/tutorials/java-concurrency/creating-and-starting-threads.html
        Thread thread = new Thread(){
            public void run(){
                //Wait a little to obtain data
                try {
                    _waitingTime += 500;
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (!IsDataAvailable(_waitingTime))
                {
                    System.out.println("Thread Running");
                    try {
                        Thread.sleep(500);
                        _waitingTime += 500;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println("Data obtained");
                System.out.println("Creating grid");

                List<Double[][][]> RSSIGrids = new ArrayList<>();
                //Create RSSI grid for each detected device
                for (String ShellyDevice: _mqttHelper.UniqueShellyList)
                {
                    List<DataPoint3D> dataPoints = GetDataPointsForDevice(ShellyDevice);
                    RSSIGrids.add(CreateRSSIGrid(width, height, resolution, numberOfFloors, dataPoints));
                }
                int[] XYLocation = GetXYZLocation(RSSIGrids);
            }
        };
        thread.start();
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

            for (String entry: _mqttHelper.UniqueShellyList)
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
            return foundEntries == _mqttHelper.UniqueShellyList.size();
        }
        else// If it takes too long, try to find at least two entries
        {
            return foundEntries > 1;
        }
    }

    private Boolean IsDataAvailable(int waitingTime)
    {
        return IsMQTTDataAvailable() && IsBeaconScannerDataAvailable(waitingTime);
    }
}
