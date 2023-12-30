package com.example.indoor_positioning_app;

import static com.example.indoor_positioning_app.BeaconScanner.REQUEST_ENABLE_BT;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {

    private List<Bitmap> _floorPlans = null;
    private List<Bitmap> _gridedFloorPlans = null;
    private ImageView _floorImageView = null;


    //relevant in case of multiple floor images
    private int _currentImageIndex = -1;
    private int _numberOfFloors = 0;

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

        //Setting up toolbar
        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        //Creating beacon scanner
        _beaconScanner = new BeaconScanner(getApplicationContext(), this);

        //Creating MQTT object to subscribe to data from Shelly devices
        _mqttHelper = new MQTTHelper(getApplicationContext());
        _mqttHelper.MQTTSubscribe();

        //Creating image handler, which will prepare floor plans images and draw curently detected devices
        _floorImageHandler = new FloorImageHandler(this);
        _floorImageHandler.mqttHelper = _mqttHelper;

        _floorPlans = _floorImageHandler.GetFloorPlans();
        _gridedFloorPlans = _floorImageHandler.GetGridedFloorPlans();

        //Setting image,first parameter is relevant in case of multiple floors


        ShowImageAtPosition(0, _isGrided, _showDevices);

        if (savedInstanceState != null){
            _isGrided = savedInstanceState.getBoolean("_isGrided");
            _showDevices = savedInstanceState.getBoolean("_showDevices");

            byte[] imageBytes = savedInstanceState.getByteArray("_displayedImage");
            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            _displayedImage = bmp;
            _floorImageView.setImageBitmap(_displayedImage);
        }



        //Algorithm object needs image width, height and resolution to create grid with interpolated values
        _algorithms = new Algorithms(_beaconScanner, _mqttHelper);
        _algorithms.imageWidth = _displayedImage.getWidth();
        _algorithms.imageHeight = _displayedImage.getHeight();
        _algorithms.numberOfFloors = _floorPlans.size();
        _algorithms.gridResolution = _floorImageHandler.GridResolution();

        //in case of multiple floors
        _numberOfFloors = _floorPlans.size();

        SetStartButtonListener();
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("_isGrided", _isGrided);
        outState.putBoolean("_showDevices", _showDevices);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        _displayedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);

        outState.putByteArray("_displayedImage", stream.toByteArray());
    }

    //Inflate toolbar with items from resource file
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    //Onclick handlers for items in toolbar menu
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

    //Function that run the interpolation algorithm, when start button is pressed
    private void SetStartButtonListener() {
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
                                if(XYZposition[2] > Integer.MIN_VALUE)//If multiple floors are present
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

    //Show image for specific floor - position
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
        _currentImageIndex = position;//for multiple floors
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