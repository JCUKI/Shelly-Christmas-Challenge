package com.example.indoor_positioning_app;

import static com.example.indoor_positioning_app.CustomDrawing.DrawGrid;
import static com.example.indoor_positioning_app.CustomDrawing.DrawPoly;

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
import java.util.Enumeration;
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
    private List<Bitmap> _gridedFloorPlans = null;
    private int _resolution = 100;

    public MQTTHelper mqttHelper = null;

    public FloorImageHandler(Context activityContext)
    {
        _activityContext = activityContext;

        PrepareFloorPlans();
    }

    public List<Bitmap> GetFloorPlans()
    {
        return _floorPlans;
    }
    public List<Bitmap> GetGridedFloorPlans()
    {
        return _gridedFloorPlans;
    }

    public int GridResolution()
    {
        return _resolution;
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
        List<List<Bitmap>> floorPlansList = LoadImagesFromGPKGs(_geoPackages);
        _floorPlans = floorPlansList.get(0);
        _gridedFloorPlans = floorPlansList.get(1);
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

    private List<List<Bitmap>> LoadImagesFromGPKGs(List<GeoPackage> geoPackages) {
        List<Bitmap> result = new ArrayList<Bitmap>();
        List<Bitmap> gridedResult = new ArrayList<Bitmap>();

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
                            customPoints[i] = new CustomDrawing.Point((int) points.get(i).getX(), (int) (height - points.get(i).getY()));
                        }
                        DrawPoly(canvas, pt, customPoints);
                    }
                }
            } finally {
                featureCursor.close();
            }
            result.add(myBitmap);

            Bitmap gridedBitmap = Bitmap.createBitmap(myBitmap);
            Canvas gridedCanvas = new Canvas(gridedBitmap);
            DrawGrid(gridedCanvas, _resolution);
            gridedResult.add(gridedBitmap);
        }
        List<List<Bitmap>> arrayOfList = new ArrayList<List<Bitmap>> ();
        arrayOfList.add(result);
        arrayOfList.add(gridedResult);

        return arrayOfList;
    }

    public Bitmap DrawCurrentDevices(Bitmap image, int floor)
    {
        if(mqttHelper==null)
        {
            return image;
        }

        Enumeration<String> dictionaryKeys = mqttHelper.mqttDataDict.keys();

        Canvas canvas = new Canvas(image);

        Paint pt = new Paint();
        pt.setStyle(Paint.Style.FILL_AND_STROKE);
        pt.setColor(Color.RED);

        while (dictionaryKeys.hasMoreElements())
        {
            String key = dictionaryKeys.nextElement();

            MQTTData data = mqttHelper.mqttDataDict.get(key);

            if(data.Z() == floor)//on the same floor
            {
                int posX = (int)Math.floor(data.X()/ _resolution)* _resolution;
                int posY = (int)Math.floor(data.Y()/ _resolution)* _resolution;

                canvas.drawRect(posX, posY, posX + _resolution, posY + _resolution, pt);
            }
        }
        return image;
    }

    public Bitmap DrawPosition(int[] position, Bitmap bitmap)
    {
        Canvas canvas = new Canvas(bitmap);

        Paint pt = new Paint();
        pt.setStyle(Paint.Style.FILL_AND_STROKE);
        pt.setColor(Color.YELLOW);

        int posX = (int)Math.floor(position[0]/ _resolution)* _resolution;
        int posY = (int)Math.floor(position[1]/ _resolution)* _resolution;

        canvas.drawRect(posX, posY, posX + _resolution, posY + _resolution, pt);

        return bitmap;
    }
}
