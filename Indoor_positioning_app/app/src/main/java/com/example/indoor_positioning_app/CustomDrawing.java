package com.example.indoor_positioning_app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class CustomDrawing {
    /**
     * Simple point
     */
    public static class Point {

        public float x = 0;
        public float y = 0;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Draw polygon
     *
     * @param canvas The canvas to draw on
     * @param polyPaint
     * @param points Polygon corner points
     */
    public static void DrawPoly(Canvas canvas, Paint polyPaint, Point[] points) {
        // line at minimum...
        if (points.length < 2) {
            return;
        }

        // path
        Path polyPath = new Path();
        polyPath.moveTo(points[0].x, points[0].y);
        int i, len;
        len = points.length;
        for (i = 0; i < len; i++) {
            polyPath.lineTo(points[i].x, points[i].y);
        }
        polyPath.lineTo(points[0].x, points[0].y);

        // draw
        canvas.drawPath(polyPath, polyPaint);
    }

    public static void DrawGrid(Canvas canvas, int  resolution)
    {
        Paint pt = new Paint();
        pt.setStyle(Paint.Style.STROKE);
        pt.setColor(Color.BLACK);

        if(resolution > canvas.getWidth())
        {
            resolution = canvas.getWidth();
        }
        else if(resolution < 1)
        {
            resolution = 1;
        }

        for (int i = 0; i < canvas.getWidth(); i+=resolution)
        {
            canvas.drawLine(i, 0, i, canvas.getHeight(), pt);
        }

        for (int i = 0; i < canvas.getHeight(); i+=resolution)
        {
            canvas.drawLine( 0, i, canvas.getWidth(), i, pt);
        }
    }
}
