package com.example.indoor_positioning_app;

import java.util.ArrayList;

//Implemented according to: https://guides.codepath.com/android/using-the-recyclerview
public class Floor {
    private String _name; //Floors may have different names
    private int _number; //Level of a building

    public Floor(String name, int number) {
        _name = name;
        _number = number;
    }

    public String getName() {
        return _name;
    }

    public int getNumber() {
        return _number;
    }

    private static int lastContactId = 0;

    public static ArrayList<Floor> createFloorList(int numberOfFloors) {
        ArrayList<Floor> floors = new ArrayList<Floor>();

        //TODO: fix this according to the actual number of floors
        for (int i = 0; i < numberOfFloors; i++) {
            floors.add(new Floor("Floor " + Integer.toString(i), i));
        }
        return floors;
    }
}