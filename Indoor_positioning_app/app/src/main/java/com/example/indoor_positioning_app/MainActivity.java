package com.example.indoor_positioning_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

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
        FloorAdapter adapter = new FloorAdapter(floors);
        // Attach the adapter to the recyclerview to populate items
        rvFloors.setAdapter(adapter);
        // Set layout manager to position the items
        rvFloors.setLayoutManager(new LinearLayoutManager(this));
    }
}