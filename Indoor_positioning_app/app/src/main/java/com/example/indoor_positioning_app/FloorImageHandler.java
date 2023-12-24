package com.example.indoor_positioning_app;

import static com.example.indoor_positioning_app.CustomDrawing.drawPoly;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.misc.IOUtils;

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

public class FloorImageHandler {

    private Context _activityContext;

    private List<GeoPackage> _geoPackages = null;
    private List<Bitmap> _floorPlans = null;
    public FloorImageHandler(Context activityContext)
    {
        _activityContext = activityContext;

        PrepareFloorPlans();
    }

    public List<Bitmap> GetFloorPlans()
    {
        return _floorPlans;
    }

    private File BytesToFile(byte[] buffer) throws IOException {
        String path = ((Activity) _activityContext).getFilesDir() + "/targetFile3.gpkg";
        Files.deleteIfExists(Paths.get(path));

        File targetFile = new File(path);
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buffer);

        IOUtils.closeQuietly(outStream);
        return targetFile;
    }

    private String[] ListGpkgsFromAssets() {
        try {
            return ((Activity) _activityContext).getAssets().list("gpkgs");
        } catch (IOException e) {
            Toast.makeText(_activityContext, "Unable to open gpkg files in assets!\n Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }

    private void PrepareFloorPlans() {
        _geoPackages = LoadFloorPlansGPKGs();
        _floorPlans = LoadImagesFromGPKGs(_geoPackages);
    }

    private List<GeoPackage> LoadFloorPlansGPKGs() {
        List<GeoPackage> result = new ArrayList<GeoPackage>();

        GeoPackageManager _gpkgManager = GeoPackageFactory.getManager(_activityContext);

        String[] gpkgFiles = ListGpkgsFromAssets();
        if (gpkgFiles.length == 0) {
            Toast.makeText(_activityContext, "No gpkg files available!", Toast.LENGTH_SHORT).show();
            return result;
        }

        // Try to import each gpkg file
        try {
            for (String file : gpkgFiles) {
                _gpkgManager = GeoPackageFactory.getManager(_activityContext);
                InputStream gpkgStream = _activityContext.getAssets().open("gpkgs/" + file);

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


}
