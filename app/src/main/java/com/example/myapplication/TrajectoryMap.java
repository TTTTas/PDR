package com.example.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.graphics.Color;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryMap extends LinearLayout {
    private LineChart trajectory;

    public TrajectoryMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.activity_trajectory, this);

        trajectory =findViewById(R.id.trajectory);
        mapInitial();
    }

    private void mapInitial()
    {
        trajectory.setBackgroundColor(Color.WHITE);
        trajectory.setDragEnabled(true);
        trajectory.getLegend().setEnabled(false);
        trajectory.getDescription().setEnabled(false);
        trajectory.setScaleEnabled(true);

        XAxis xAxis = trajectory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);

        YAxis leftYAxis = trajectory.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(Color.LTGRAY);
        YAxis rightYAxis= trajectory.getAxisRight();
        rightYAxis.setEnabled(false);

        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0,0));

        LineDataSet dataSet = new LineDataSet(entries, "trajectory");
        dataSet.setColor(Color.parseColor("#03A9F4"));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(4);

        LineData lineData = new LineData(dataSet);
        trajectory.setData(lineData);
    }

    public void drawMap(List<double[]> pos)
    {
        LineData lineData = trajectory.getLineData();
        LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
        for(int i=0;i<pos.size();i++)
        {
            double[] p = pos.get(i);
            dataSet.addEntry(new Entry((float)p[0],(float)p[1]));
        }
        lineData.notifyDataChanged();
        trajectory.notify();
        trajectory.moveViewToX(lineData.getEntryCount());
    }
}
