package com.example.indoor_positioning_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {

    ArrayList<Floor> floors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFloorsRecycleView();
    }

    private void initializeFloorsRecycleView()
    {
        // Lookup the recyclerview in activity layout
        RecyclerView rvFloors = (RecyclerView) findViewById(R.id.rvFloors);

        // Initialize contacts
        floors = Floor.createFloorList(20);
        // Create adapter passing in the sample user data
        FloorAdapter adapter = new FloorAdapter(floors, this);
        // Attach the adapter to the recyclerview to populate items
        rvFloors.setAdapter(adapter);
        // Set layout manager to position the items
        rvFloors.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onItemClick(int position) {
        TextView textView = (TextView) findViewById(R.id.currentImage);
        textView.setText("Floor" + Integer.toString(position));
    }
}