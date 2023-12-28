package com.example.indoor_positioning_app;


import static androidx.core.app.ActivityCompat.requestPermissions;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;


//This was taken from: https://github.com/hipstermartin/indoor-positioning-system/tree/main/Beacon-scanner
public class BeaconServiceNew extends Service implements RegionsBeaconService {

    private final IBinder mBinder = new LocalBinder();
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    List<BluetoothDevice> deviceList = new ArrayList<>();
    private Boolean enabled = false;
    BeaconListener listener;
    BluetoothAdapter.LeScanCallback callback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBluetoothAdapter != null) {
            startBlueService();
        }
        return mBinder;
    }

    private void startBlueService() {
        if (mBluetoothAdapter.isEnabled()) {
            enabled = true;
            setCallback();
        } else {
            Log.i("no adapter", "");
        }
    }

    private void setCallback() {
        callback = new BluetoothAdapter.LeScanCallback() {
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                listener.scaning(true);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                device.getUuids();
                String Name;
                Name = (String) device.getName();

                if(Name == null)
                {
                    return;
                }
                Name = Name.toUpperCase();
                if(!Name.contains("SHELLY"))
                {
                    return;
                }

                String mAdress = device.getAddress();

                double distance;
                if (rssi == 0) {
                    distance = -1.0;
                }

                //Bellow formulas were used to calculate distance in
                https://github.com/hipstermartin/indoor-positioning-system
                // distance = (0.51120) * Math.pow(ratio, 6.100) + 0.115;
                distance = (0.882909233) * Math.pow((rssi / -58), 4.57459326) + 0.045275821;

                listener.beaconRecieved("", -1, -1, distance, Name, rssi, mAdress);
            }
        };
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BeaconServiceNew getService() {
            // Return this instance of LocalService so clients can call public methods
            return BeaconServiceNew.this;
        }
    }

    @Override
    public void manualScan() {
        if (enabled) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                //return;

                //shouldShowRequestPermissionRationale()//https://developer.android.com/training/permissions/requesting#java



                //ActivityCompat.requestPermissions(MainActivity.this,{android.Manifest.permission.BLUETOOTH_SCAN},0);
                return;
            }
            mBluetoothAdapter.startLeScan(callback);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    listener.scaning(false);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mBluetoothAdapter.stopLeScan(callback);
                }
            }, 4000);
        }
    }

    @Override
    public void setListener(BeaconListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isBlueToothOn() {
        boolean on = false;

        if(mBluetoothAdapter != null){
            on = mBluetoothAdapter.isEnabled();
        }

        return on;
    }

    @Override
    public void restart() {
        enabled = true;
        setCallback();
    }

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
