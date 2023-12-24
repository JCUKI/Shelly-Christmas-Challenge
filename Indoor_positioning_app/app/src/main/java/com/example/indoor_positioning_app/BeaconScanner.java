package com.example.indoor_positioning_app;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;

//Majority of code was taken from: https://github.com/hipstermartin/indoor-positioning-system/tree/main/Beacon-scanner
public class BeaconScanner {

    public RegionsBeaconService _regionsNewBeaconService;
    private Context _mainActivityContext;
    private ServiceConnection _mConnection;

//    public BeaconScanner(Context applicationContext, Context activityContext, ServiceConnection mConnection)
//    {
//        _mConnection = mConnection;
//
//        Intent intent = new Intent(applicationContext, BeaconServiceNew.class);
//        applicationContext.bindService(intent, _mConnection, Context.BIND_AUTO_CREATE);
//
//        String[] PERMISSIONS = {android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT};
//        ActivityCompat.requestPermissions((Activity) activityContext, PERMISSIONS,0);
//    }
//
//    public void StartManualScan()
//    {
//        _regionsNewBeaconService.manualScan();
//    }

}
