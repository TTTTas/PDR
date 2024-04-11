package com.example.myapplication;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

public class MapData {
    private List<Entry> start_point;
    private List<Entry> path_point;
    private int new_pts;
    private List<Entry> end_point;
    private boolean is_stop;
    private float x_Min,x_Max,y_Min,y_Max;

    public MapData()
    {
        initial();
    }

    public MapData(String path)
    {
        load_file(path);
    }

    public MapData(LineChart map)
    {
        initial();
        InitialMap(map);
    }

    private void initial()
    {
        start_point = new ArrayList<Entry>();
        path_point = new ArrayList<Entry>();
        end_point = new ArrayList<Entry>();
        start_point.add(new Entry(0,0));
        path_point.add(new Entry(0,0));
        end_point.add(new Entry(0,0));
        x_Min=0;
        y_Min=0;
        x_Max=0;
        y_Max=0;
        is_stop=false;
    }

    public void InitialMap(LineChart map)
    {
        map.setBackgroundColor(Color.WHITE);
        map.setDragEnabled(true);
        map.getLegend().setEnabled(false);
        map.getDescription().setEnabled(false);
        map.setScaleEnabled(true);
        map.setTouchEnabled(true);

        XAxis xAxis = map.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);

        YAxis leftYAxis = map.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setInverted(false);
        leftYAxis.setGridColor(Color.LTGRAY);
        YAxis rightYAxis = map.getAxisRight();
        rightYAxis.setEnabled(false);

        LineDataSet dataSetStart = new LineDataSet(start_point, "Start");
        dataSetStart.setCircleColor(Color.parseColor("#03A9F4"));
        dataSetStart.setDrawCircles(true);
        dataSetStart.setCircleRadius(6);
        dataSetStart.setDrawCircleHole(false);
        dataSetStart.setDrawValues(false);
        dataSetStart.setLineWidth(0);

        LineDataSet dataSet = new LineDataSet(path_point, "trajectory");
        dataSet.setColor(Color.parseColor("#EEDC82"));
        dataSet.setDrawCircles(true);
        dataSet.setDrawCircleHole(false);
        dataSet.setCircleColor(Color.parseColor("#FDF447"));
        dataSetStart.setCircleRadius(5);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(4);

        LineData lineData = new LineData(dataSet, dataSetStart);
        map.setData(lineData);
        lineData.notifyDataChanged();
        map.notifyDataSetChanged();
        map.invalidate();
    }


    public void load_file(String path)
    {

    }

    public void save_file(String path)
    {

    }

    public void load_map(@NonNull LineChart map)
    {
        map.getLineData().clearValues();
        InitialMap(map);
    }
    public void reset(LineChart map)
    {
        start_point.clear();
        path_point.clear();
        end_point.clear();
        start_point.add(new Entry(0,0));
        path_point.add(new Entry(0,0));
        end_point.add(new Entry(0,0));
        x_Min=0;
        y_Min=0;
        x_Max=0;
        y_Max=0;
        is_stop=false;
        load_map(map);
    }

    public void add_data(@NonNull List<double[]> pos)
    {
        new_pts=pos.size();
        for(int i = 0;i<new_pts;i++)
        {
            double[] p = pos.get(i);
            float x=(float) p[0];
            float y=(float) p[1];
            if(x_Max<x)x_Max=x;
            if(x_Min>x)x_Min=x;
            if(y_Max<y)y_Max=y;
            if(y_Min>y)y_Min=y;
            path_point.add(new Entry((float) p[0], (float) p[1]));
        }
    }

    public void change_stop_flag()
    {
        is_stop=!is_stop;
    }

    public void invalid_map(@NonNull LineChart map)
    {
        LineData lineData = map.getLineData();
        if(is_stop)
        {
            end_point.clear();
            end_point.add(path_point.get(path_point.size()-1));
            LineDataSet dataSetStop = new LineDataSet(end_point, "Stop");
            dataSetStop.setCircleColor(Color.parseColor("#EC4F44"));
            dataSetStop.setDrawCircles(true);
            dataSetStop.setDrawCircleHole(false);
            dataSetStop.setCircleRadius(6);
            dataSetStop.setDrawValues(false);
            dataSetStop.setLineWidth(0);
            lineData.addDataSet(dataSetStop);
            is_stop=false;
        }
        lineData.notifyDataChanged();
        map.notifyDataSetChanged();
        float dx=x_Max-x_Min;
        float dy = y_Max-y_Min;
        map.getXAxis().setAxisMinimum((float) (x_Min - dx*0.2));
        map.getXAxis().setAxisMaximum((float) (x_Max + dx*0.2));
        map.getAxisLeft().setAxisMinimum((float) (y_Min - dy*0.2));
        map.getAxisLeft().setAxisMaximum((float) (y_Max + dy*0.2));
        map.invalidate();
    }
}
