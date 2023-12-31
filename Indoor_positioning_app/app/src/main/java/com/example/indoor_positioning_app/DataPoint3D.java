package com.example.indoor_positioning_app;

// Sample data point class representing 3D coordinates and values
public class DataPoint3D {
    private final double _x;
    private final double _y;
    private final double _z;
    private final double _value;

    public DataPoint3D(double x, double y, double z, double value) {
        this._x = x;
        this._y = y;
        this._z = z;
        this._value = value;
    }

    public double getX() {
        return _x;
    }

    public double getY() {
        return _y;
    }

    public double getZ() {
        return _z;
    }

    public double getValue() {
        return _value;
    }
}
