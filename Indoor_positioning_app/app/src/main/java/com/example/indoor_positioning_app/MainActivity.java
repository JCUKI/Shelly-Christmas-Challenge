package com.example.indoor_positioning_app;

import static com.example.indoor_positioning_app.BeaconScanner.REQUEST_ENABLE_BT;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _floorImageView = (ImageView) findViewById(R.id.currentImage);

        _beaconScanner = new BeaconScanner(getApplicationContext(), this);

        SetBluetoothScanListener();

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

        InitializeFloorsRecycleView(_floorPlans.size());
        SetOnClickListenerToGridButton();
        SetOnClickListenerToShowDevicesButton();
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