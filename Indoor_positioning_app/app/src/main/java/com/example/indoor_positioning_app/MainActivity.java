package com.example.indoor_positioning_app;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.example.indoor_positioning_app.CustomDrawing.drawPoly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.BoringLayout;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import com.j256.ormlite.misc.IOUtils;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.QoS;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageFactory;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.sf.Geometry;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {

    private static final int REQUEST_ENABLE_BT = 312;
    private ArrayList<Floor> _floorItems = null;
    private List<GeoPackage> _geoPackages = null;
    private List<Bitmap> _floorPlans = null;
    private ImageView _floorImageView = null;
    private MqttAndroidClient mqttAndroidClient = null;
    RegionsBeaconService regionsNewBeaconService;
    Boolean mBeaconBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _floorImageView = (ImageView) findViewById(R.id.currentImage);

        //Bluetooth - TODO: add to seperate file
        setListener();
        Intent intent = new Intent(getApplicationContext(), BeaconServiceNew.class);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        String[] PERMISSIONS = {android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT};
        ActivityCompat.requestPermissions((Activity) this, PERMISSIONS, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        66667);
            }
        }
        //regionsNewBeaconService.manualScan();

//        PrepareFloorPlans();
//        ShowImageAtPosition(0);
//        InitializeFloorsRecycleView(_floorPlans.size());
//
//
//        MQTTSubscribeDemo();
    }

    private void setListener() {

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regionsNewBeaconService.manualScan();
            }
        });
    }

    private void InitializeFloorsRecycleView(int numberOfFloors) {
        RecyclerView floorsRecyclerView = (RecyclerView) findViewById(R.id.rvFloors);
        _floorItems = Floor.createFloorList(numberOfFloors);
        // Create adapter passing in the sample user data
        FloorAdapter adapter = new FloorAdapter(_floorItems, this);
        // Attach the adapter to the recyclerview to populate items
        floorsRecyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        floorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public File BytesToFile(byte[] buffer) throws IOException {
        String path = MainActivity.this.getFilesDir() + "/targetFile3.gpkg";
        Files.deleteIfExists(Paths.get(path));

        File targetFile = new File(path);
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buffer);

        IOUtils.closeQuietly(outStream);
        return targetFile;
    }

    private String[] ListGpkgsFromAssets() {
        try {
            return getAssets().list("gpkgs");
        } catch (IOException e) {
            Toast.makeText(this, "Unable to open gpkg files in assets!\n Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }

    private void PrepareFloorPlans() {
        _geoPackages = LoadFloorPlansGPKGs();
        _floorPlans = LoadImagesFromGPKGs(_geoPackages);
    }

    private List<GeoPackage> LoadFloorPlansGPKGs() {
        List<GeoPackage> result = new ArrayList<GeoPackage>();

        GeoPackageManager _gpkgManager = GeoPackageFactory.getManager(this);

        String[] gpkgFiles = ListGpkgsFromAssets();
        if (gpkgFiles.length == 0) {
            Toast.makeText(this, "No gpkg files available!", Toast.LENGTH_SHORT).show();
            return result;
        }

        // Try to import each gpkg file
        try {
            for (String file : gpkgFiles) {
                _gpkgManager = GeoPackageFactory.getManager(this);
                InputStream gpkgStream = getAssets().open("gpkgs/" + file);

                gpkgStream.reset();
                byte[] arr = new byte[gpkgStream.available()];
                DataInputStream dataInputStream = new DataInputStream(gpkgStream);
                dataInputStream.readFully(arr);

                try {
                    boolean imported = _gpkgManager.importGeoPackage("file_" + file, BytesToFile(arr));
                } catch (Exception ex) {
                }
            }

            List<String> databases = _gpkgManager.databases();
            Collections.sort(databases);
            for (String database : databases) {
                GeoPackage geoPackage = _gpkgManager.open(database);
                result.add(geoPackage);
            }
        } catch (Exception ex) {
            Log.d("GPKG", "GPKGmanager.importGeoPackage error");
        }
        return result;
    }

    private List<Bitmap> LoadImagesFromGPKGs(List<GeoPackage> geoPackages) {
        List<Bitmap> result = new ArrayList<Bitmap>();

        Paint pt = new Paint();
        pt.setStyle(Paint.Style.STROKE);
        pt.setColor(Color.BLUE);

        for (GeoPackage geoPackage : geoPackages) {
            // Query Features
            List<String> features = geoPackage.getFeatureTables();
            String featureTable = features.get(0);
            FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
            FeatureCursor featureCursor = featureDao.queryForAll();

            BoundingBox bbox = featureDao.getBoundingBox();
            int height = (int) bbox.getMaxLatitude();
            int width = (int) bbox.getMaxLongitude();

            Bitmap myBitmap = Bitmap.createBitmap(width + 10, height + 10, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(myBitmap);
            canvas.drawColor(Color.WHITE);

            try {
                for (FeatureRow featureRow : featureCursor) {
                    GeoPackageGeometryData geometryData = featureRow.getGeometry();

                    if (geometryData != null && !geometryData.isEmpty()) {

                        Geometry geometry = geometryData.getGeometry();
                        MultiPolygon polygon = (MultiPolygon) geometry;
                        List<Point> points = polygon.getPolygon(0).getRing(0).getPoints();

                        CustomDrawing.Point[] customPoints = new CustomDrawing.Point[points.size()];

                        for (int i = 0; i < points.size(); i++) {
                            customPoints[i] = new CustomDrawing.Point((int) points.get(i).getX(), (int) points.get(i).getY());
                        }
                        drawPoly(canvas, pt, customPoints);
                    }
                }
            } finally {
                featureCursor.close();
            }
            result.add(myBitmap);
        }
        return result;
    }

    private void ShowImageAtPosition(int position) {
        _floorImageView.setImageBitmap(_floorPlans.get(position));
    }

    @Override
    public void onItemClick(int position) {
        ShowImageAtPosition(position);
    }

    private void MQTTSubscribeDemo() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), "tcp://192.168.5.15:1883", MqttClient.generateClientId());

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    Log.d("MQTT", "Reconnected: " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    SubscribeToTopic();
                } else {
                    Log.d("MQTT", "Connected: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        Log.d("MQTT", "Connected");

        mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                SubscribeToTopic();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("MQTT", "Failed to connect");
            }
        });
    }

    void SubscribeToTopic() {
        mqttAndroidClient.subscribe("ShellyTopic", QoS.AtLeastOnce.getValue(), null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "onSuccess: ");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("MQTT", "onFailure: ");
            }
        });
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            if (service instanceof BeaconServiceOld.LocalBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                BeaconServiceOld.LocalBinder binder = (BeaconServiceOld.LocalBinder) service;
                regionsNewBeaconService = binder.getService();
                mBeaconBound = true;
            } else if (service instanceof BeaconServiceNew.LocalBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                BeaconServiceNew.LocalBinder binder = (BeaconServiceNew.LocalBinder) service;
                regionsNewBeaconService = binder.getService();
                regionsNewBeaconService.setListener(new BeaconListener() {


                    @Override
                    public void beaconRecieved(String uuid, int minor, int mayor, double distance, String name, int rssi, String mAdress) {
                        Log.d("beaconRecieved", Integer.toString(rssi));
                    }

                    @Override
                    public void scaning(Boolean scanning) {
                        Log.d("Scanning", Boolean.toString(scanning));
                    }
                });
                if (!regionsNewBeaconService.isBlueToothOn()) {
                    askForBlueTooth();
                }
                mBeaconBound = true;
            }


        }


        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBeaconBound = false;
        }
    };

    private void askForBlueTooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            regionsNewBeaconService.restart();
        }
    }
}