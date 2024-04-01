package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.jar.Attributes;

public class DataCollectView extends LinearLayout {
    private TextView accelerometerData, gyroscopeData, magnetometerData;
    private LineChart accelchart, gyrochart, magnchart;
    private TextView collectionTimeTextView;
    private ArrayList<Entry>accelx, accely,accelz,gyrox,gyroy,gyroz,magnx,magny,magnz;
    private boolean isupdate = false;

    public DataCollectView(Context context) {
        super(context);
    }
    public DataCollectView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.data_collect_view, this);

        accelchart = findViewById(R.id.accel_chart);
        gyrochart = findViewById(R.id.gyro_chart);
        magnchart = findViewById(R.id.magn_chart);
        accelerometerData = findViewById(R.id.accelerometerData);
        gyroscopeData = findViewById(R.id.gyroscopeData);
        magnetometerData = findViewById(R.id.magnetometerData);
        collectionTimeTextView = findViewById(R.id.collectionTime);

        chartInitial();
    }


    private void chartInitial(){
        // Initialize each chart
        initializeLineChart(accelchart);
        initializeLineChart(gyrochart);
        initializeLineChart(magnchart);
        accelchart.setScaleEnabled(true);
        gyrochart.setScaleEnabled(true);
        magnchart.setScaleEnabled(true);
        accelchart.getLegend().setForm(Legend.LegendForm.LINE);
        gyrochart.getLegend().setForm(Legend.LegendForm.LINE);
        magnchart.getLegend().setForm(Legend.LegendForm.LINE);
        accelchart.getLegend().setXEntrySpace(12);
        gyrochart.getLegend().setXEntrySpace(12);
        magnchart.getLegend().setXEntrySpace(12);

        ArrayList<Entry> entries1 = new ArrayList<>();
        ArrayList<Entry> entries2 = new ArrayList<>();
        ArrayList<Entry> entries3 = new ArrayList<>();

        entries1.add(new Entry(0,0));
        entries2.add(new Entry(0,0));
        entries3.add(new Entry(0,0));

        LineDataSet dataSet1 = new LineDataSet(entries1, "X");
        LineDataSet dataSet2 = new LineDataSet(entries2, "Y");
        LineDataSet dataSet3 = new LineDataSet(entries3, "Z");

        dataSet1.setColor(Color.parseColor("#03A9F4"));
        dataSet2.setColor(Color.parseColor("#EC4F44"));
        dataSet3.setColor(Color.parseColor("#FF9800"));
        dataSet1.setDrawValues(false);
        dataSet2.setDrawValues(false);
        dataSet3.setDrawValues(false);
        dataSet1.setDrawCircles(false);
        dataSet2.setDrawCircles(false);
        dataSet3.setDrawCircles(false);
        LineData lineData = new LineData(dataSet1, dataSet2, dataSet3);

        accelchart.setData(lineData);
        accelchart.invalidate();
        gyrochart.setData(lineData);
        gyrochart.invalidate();
        magnchart.setData(lineData);
        magnchart.invalidate();
    }

    private static void initializeLineChart(LineChart chart) {
        // Set background color
        chart.setBackgroundColor(Color.WHITE);

        // Enable drag
        chart.setDragEnabled(true);
        chart.getDescription().setEnabled(false);

        // Customize X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        //xAxis.setXOffset(0);

        // Customize Y axis
        YAxis leftYAxis = chart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(Color.LTGRAY);
        YAxis rightYAxis = chart.getAxisRight();
        rightYAxis.setEnabled(false); // Disable right Y axis
    }

    public static void updateChartData(ArrayList<Entry> x, ArrayList<Entry> y, ArrayList<Entry> z, LineChart chart) {
        // Get the current LineData from the chart
        LineData lineData = chart.getLineData();

        if (lineData != null) {
            // Get the LineDataSet for each line
            LineDataSet dataSetX = (LineDataSet) lineData.getDataSetByIndex(0);
            LineDataSet dataSetY = (LineDataSet) lineData.getDataSetByIndex(1);
            LineDataSet dataSetZ = (LineDataSet) lineData.getDataSetByIndex(2);

            // Add new entries to each dataset
            addEntryToDataSet(x, dataSetX);
            addEntryToDataSet(y, dataSetY);
            addEntryToDataSet(z, dataSetZ);

            // Notify chart data has changed
            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();

            // Move to the latest entry
            chart.moveViewToX(lineData.getEntryCount());
        }
    }

    private static void addEntryToDataSet(@NonNull ArrayList<Entry>value, LineDataSet dataSet) {
        // Add a new entry to the dataset
        for(int i=0;i< value.size();i++)
        {
            dataSet.addEntry(value.get(i));
        }
    }

    public void update(float time, float x, float y, float z, int type, String dataString)
    {
        if(accelchart.getLineData().getEntryCount()>300)
        {
            int count=accelx.size();
            if(count>50)
                isupdate=true;
            else
            {
                isupdate=false;
                switch (type) {
                    case Sensor.TYPE_ACCELEROMETER:
                        accelx.add(new Entry(time, x));
                        accely.add(new Entry(time, y));
                        accelz.add(new Entry(time, z));
                        if (isupdate)
                            updateChartData(accelx,accely, accelz, accelchart);
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        gyrox.add(new Entry(time, x));
                        gyroy.add(new Entry(time, y));
                        gyroz.add(new Entry(time, z));
                        if (isupdate)
                            updateChartData(gyrox,gyroy, gyroz, gyrochart);
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        magnx.add(new Entry(time, x));
                        magny.add(new Entry(time, y));
                        magnz.add(new Entry(time, z));
                        if (isupdate)
                            updateChartData(magnx,magny, magnz, magnchart);
                        break;

                }
            }
        }

        // 根据传感器类型更新UI
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerData.setText(dataString);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeData.setText(dataString);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetometerData.setText(dataString);
                break;
        }
        // 更新已采集时间的TextView
        collectionTimeTextView.setText(String.format("已采集时间: %.3f秒", time));
    }

}
