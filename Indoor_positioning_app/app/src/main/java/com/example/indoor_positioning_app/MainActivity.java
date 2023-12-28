package com.example.indoor_positioning_app;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.example.indoor_positioning_app.BeaconScanner.REQUEST_ENABLE_BT;

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
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    private List<Bitmap> _floorPlans = null;
    private List<Bitmap> _gridedFloorPlans = null;
    private ImageView _floorImageView = null;

    private int _currentImageIndex = -1;
    private boolean _isGrided = false;
    private boolean _showDevices = false;
    private BeaconScanner beaconScanner = null;
    private MQTTHelper mqttHelper = null;

    private FloorImageHandler floorImageHandler = null;
    private Algorithms _algorithms;
    private Bitmap _displayedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _floorImageView = (ImageView) findViewById(R.id.currentImage);

        beaconScanner = new BeaconScanner(getApplicationContext(), this);
        SetBluetoothScanListener();

        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.MQTTSubscribe();

        _algorithms = new Algorithms(beaconScanner, mqttHelper);

        floorImageHandler = new FloorImageHandler(this);
        _floorPlans = floorImageHandler.GetFloorPlans();
        _gridedFloorPlans = floorImageHandler.GetGridedFloorPlans();

        floorImageHandler.mqttHelper = mqttHelper;

        ShowImageAtPosition(0, _isGrided, false);

        InitializeFloorsRecycleView(_floorPlans.size());
        SetOnClickListenerToGridButton();
        SetOnClickListenerToShowDevicesButton();
    }

    private void SetBluetoothScanListener() {
        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beaconScanner.Scan();

                int[] position = _algorithms.GetPositionWithIDW(_displayedImage.getWidth(), _displayedImage.getHeight(), _floorPlans.size(), floorImageHandler.GridResolution());

                //DUMY POSITION
//                int[] position = new int[3];//xyz
//                position[0] = 800;
//                position[1] = 300;
//                position[2] = 1;

                if(position[2] > Integer.MIN_VALUE)
                {
                    ShowImageAtPosition(position[2], _isGrided, _showDevices);
                }

                _displayedImage = floorImageHandler.DrawPosition(position, _displayedImage);
                _floorImageView.setImageBitmap(_displayedImage);
            }
        });
    }

    private void InitializeFloorsRecycleView(int numberOfFloors) {
        RecyclerView floorsRecyclerView = (RecyclerView) findViewById(R.id.rvFloors);
        ArrayList<Floor> _floorItems = Floor.createFloorList(numberOfFloors);
        // Create adapter passing in the sample user data
        FloorAdapter adapter = new FloorAdapter(_floorItems, this);
        // Attach the adapter to the recyclerview to populate items
        floorsRecyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        floorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void ShowImageAtPosition(int position, boolean isGrided, boolean showDevices) {
        if(isGrided)
        {
            _displayedImage = Bitmap.createBitmap(_gridedFloorPlans.get(position));
        }
        else {
            _displayedImage = Bitmap.createBitmap(_floorPlans.get(position));
        }

        if(showDevices)
        {
            _displayedImage = floorImageHandler.drawCurrentDevices(_displayedImage, position);
        }

        _floorImageView.setImageBitmap(_displayedImage);
        _currentImageIndex = position;
    }

    @Override
    public void onItemClick(int position) {
        ShowImageAtPosition(position, _isGrided, _showDevices);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            beaconScanner.RestartBeaconService();
        }
    }

    private void SetOnClickListenerToGridButton()
    {
        Button button = (Button) findViewById(R.id.showGridButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!_isGrided)
                {
                    button.setText("Hide grid");
                    _isGrided = true;
                }
                else{
                    button.setText("Show grid");
                    _isGrided = false;
                }
                ShowImageAtPosition(_currentImageIndex, _isGrided, _showDevices);
            }
        });
    }

    private void SetOnClickListenerToShowDevicesButton()
    {
        Button button = (Button) findViewById(R.id.showDevicesButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!_showDevices)
                {
                    button.setText("Hide devices");
                    _showDevices = true;
                }
                else{
                    button.setText("Show devices");
                    _showDevices = false;
                }
                ShowImageAtPosition(_currentImageIndex, _isGrided, _showDevices);
            }
        });
    }
}