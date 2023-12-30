package com.example.indoor_positioning_app;

import static com.example.indoor_positioning_app.BeaconScanner.REQUEST_ENABLE_BT;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {

    private List<Bitmap> _floorPlans = null;
    private List<Bitmap> _gridedFloorPlans = null;
    private ImageView _floorImageView = null;

    private int _currentImageIndex = -1;
    private Bitmap _displayedImage;

    private boolean _isGrided = false;
    private boolean _showDevices = false;

    private BeaconScanner _beaconScanner = null;
    private MQTTHelper _mqttHelper = null;
    private FloorImageHandler _floorImageHandler = null;
    private Algorithms _algorithms;

    private int _numberOfFloors = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _floorImageView = (ImageView) findViewById(R.id.currentImage);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        _beaconScanner = new BeaconScanner(getApplicationContext(), this);

//        SetBluetoothScanListener();

        _mqttHelper = new MQTTHelper(getApplicationContext());
        _mqttHelper.MQTTSubscribe();

        _floorImageHandler = new FloorImageHandler(this);
        _floorImageHandler.mqttHelper = _mqttHelper;

        _floorPlans = _floorImageHandler.GetFloorPlans();
        _gridedFloorPlans = _floorImageHandler.GetGridedFloorPlans();

        //Setting image, recycle view and OnClickListeneres
        ShowImageAtPosition(0, _isGrided, false);

        _algorithms = new Algorithms(_beaconScanner, _mqttHelper);
        _algorithms.imageWidth = _displayedImage.getWidth();
        _algorithms.imageHeight = _displayedImage.getHeight();
        _algorithms.numberOfFloors = _floorPlans.size();
        _algorithms.gridResolution = _floorImageHandler.GridResolution();
        _mqttHelper.algorithmsObject = _algorithms;


        _numberOfFloors = _floorPlans.size();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.showDevicesButton)
        {
            ShowDevices(item);
        }
        else if (item.getItemId() == R.id.showGridButton)
        {
            ShowGrid(item);
        }

        return true;
    }

    private void SetBluetoothScanListener() {
        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler(); // to update UI  from background
                Thread thread = new Thread(){
                    @Override
                    public void run(){
                        int _waitingTime = 0;
                        while (!_algorithms.IsDataAvailable(-1))
                        {
                            try {
                        sleep(500);
                                _waitingTime+= 500;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                        }
                        System.out.println("Data obtained");
                        for(String shelly: _mqttHelper.UniqueShellyList())
                        {
                            _algorithms.FillRSSIGrid(shelly);
                        }
                        int[] XYZposition = _algorithms.GetCurrentPositionWithIDW();

                        handler.post(new Runnable() {//update UI
                            @Override
                            public void run() {
                                if(XYZposition[2] > Integer.MIN_VALUE)
                                {
                                    ShowImageAtPosition(XYZposition[2], _isGrided, _showDevices);
                                }

                                _displayedImage = _floorImageHandler.DrawPosition(XYZposition, _displayedImage);
                                _floorImageView.setImageBitmap(_displayedImage);
                            }
                        });
                    }
                };
                thread.start();
            }
        });
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
            _displayedImage = _floorImageHandler.DrawCurrentDevices(_displayedImage, position);
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
            _beaconScanner.RestartBeaconService();
        }
    }

    private void ShowGrid(MenuItem item)
    {
        if(!_isGrided)
        {
            item.setTitle("Hide grid");
            _isGrided = true;
        }
        else{
            item.setTitle("Show grid");
            _isGrided = false;
        }
        item.setChecked(_isGrided);
        ShowImageAtPosition(_currentImageIndex, _isGrided, _showDevices);
    }

    private void ShowDevices(MenuItem item)
    {
        if(!_showDevices)
        {
            item.setTitle("Hide devices");
            _showDevices = true;
        }
        else{
            item.setTitle("Show devices");
            _showDevices = false;
        }
        item.setChecked(_showDevices);
        ShowImageAtPosition(_currentImageIndex, _isGrided, _showDevices);
    }
}