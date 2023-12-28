package com.example.indoor_positioning_app;


import static androidx.core.app.ActivityCompat.startActivityForResult;

import androidx.activity.ComponentActivity;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.Hashtable;

//Majority of code was taken from: https://github.com/hipstermartin/indoor-positioning-system/tree/main/Beacon-scanner
public class BeaconScanner {

    public static final int REQUEST_ENABLE_BT = 312;
    private RegionsBeaconService regionsNewBeaconService;
    private Context _activityContext;

    public Hashtable<String, BLEBeaconData> beaconDataDict = null;

   public BeaconScanner(Context applicationContext, Context activityContext)
   {
       _activityContext = activityContext;

       beaconDataDict = new Hashtable<String, BLEBeaconData>();

       Intent intent = new Intent(applicationContext, BeaconServiceNew.class);
       applicationContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

       String[] PERMISSIONS = {android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT};
       ActivityCompat.requestPermissions((Activity) activityContext, PERMISSIONS, 0);

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           if (applicationContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions((Activity) activityContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},66667);
           }
       }
   }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            if (service instanceof BeaconServiceOld.LocalBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                BeaconServiceOld.LocalBinder binder = (BeaconServiceOld.LocalBinder) service;
                regionsNewBeaconService = binder.getService();
            } else if (service instanceof BeaconServiceNew.LocalBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                BeaconServiceNew.LocalBinder binder = (BeaconServiceNew.LocalBinder) service;
                regionsNewBeaconService = binder.getService();
                regionsNewBeaconService.setListener(new BeaconListener() {
                    @Override
                    public void beaconRecieved(String uuid, int minor, int mayor, double distance, String name, int rssi, String mAdress) {
                        BLEBeaconData data  = new BLEBeaconData(distance, name, rssi, mAdress);
                        beaconDataDict.put(name, data);
                        Log.d("beaconRecieved", "Name: " + name + ", rssi: " + Integer.toString(rssi));
                    }

                    @Override
                    public void scaning(Boolean scanning) {
                        Log.d("Scanning", Boolean.toString(scanning));
                    }
                });
                if (!regionsNewBeaconService.isBlueToothOn()) {
                    askForBlueTooth();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("onServiceDisconnected", arg0.toString());
        }
    };

    private void askForBlueTooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(_activityContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        ((Activity)_activityContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public void Scan()
    {
        regionsNewBeaconService.manualScan();
    }

    public void RestartBeaconService()
    {
        regionsNewBeaconService.restart();
    }
}
