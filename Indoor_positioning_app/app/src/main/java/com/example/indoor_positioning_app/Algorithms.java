package com.example.indoor_positioning_app;

import java.util.ArrayList;
import java.util.List;

//IDW was generated using ChatGPT
public class Algorithms {

    public Algorithms()
    {
    }
    // Function to calculate inverse distance weighting interpolation in 3D
    public static double inverseDistanceWeighting3D(List<DataPoint3D> dataPoints, double x, double y, double z, int power) {
        double numerator = 0.0;
        double denominator = 0.0;

        for (DataPoint3D point : dataPoints) {
            double distance = calculateDistance3D(point.getX(), point.getY(), point.getZ(), x, y, z);

            if (distance == 0) {
                // Return the value directly if it's at a sample point
                return point.getValue();
            }

            double weight = 1.0 / Math.pow(distance, power);
            numerator += weight * point.getValue();
            denominator += weight;
        }

        if (denominator == 0) {
            // Avoid division by zero
            return 0.0;
        }

        return numerator / denominator;
    }

    // Helper function to calculate Euclidean distance in 3D
    private static double calculateDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2) + Math.pow((z2 - z1), 2));
    }

    // Sample data point class representing 3D coordinates and values
    static class DataPoint3D {
        private final double x;
        private final double y;
        private final double z;
        private final double value;

        public DataPoint3D(double x, double y, double z, double value) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.value = value;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getValue() {
            return value;
        }
    }
    public void DemoIDW()
    {
        // Sample data points in 3D
        List<DataPoint3D> dataPoints = new ArrayList<>();
        dataPoints.add(new DataPoint3D(1.0, 1.0, 1.0, 2.0));
        dataPoints.add(new DataPoint3D(2.0, 2.0, 2.0, 3.0));
        dataPoints.add(new DataPoint3D(3.0, 3.0, 3.0, 5.0));
        // Add more data points as needed

        // Point for which interpolation is needed in 3D
        double xInterpolated = 2.5;
        double yInterpolated = 2.5;
        double zInterpolated = 2.5;

        // Interpolate the value at the specified point using inverse distance weighting in 3D
        int power = 2; // Power parameter for inverse distance weighting
        double interpolatedValue = inverseDistanceWeighting3D(dataPoints, xInterpolated, yInterpolated, zInterpolated, power);

        System.out.println("Interpolated value at (" + xInterpolated + ", " + yInterpolated + ", " + zInterpolated + "): " + interpolatedValue);
    }
}
