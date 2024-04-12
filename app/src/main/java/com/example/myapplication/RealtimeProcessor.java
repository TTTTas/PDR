package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.util.ArrayList;
import java.util.List;

public class RealtimeProcessor {
    // 航向角
    private double Yaw;
    // 是否探测到脚步
    private boolean isStep;
    // 步长
    private double Step_length;
    // 上一次更新坐标
    private double[] Pos;

    public RealtimeProcessor(){
        Yaw=0.0;
        isStep = false;
        Step_length=0.75;
        Pos[0]=0.0;
        Pos[1]=0.0;
    }

    private void processGyro(SensorEvent event){
        // 添加代码
    }

    private void processAccel(SensorEvent event){
        // 添加代码
    }

    private void invalid_pos(List<double[]> pos){
        // 添加代码
    }

    public List<double[]> processRealTime(SensorEvent event){
        List<double[]> pos=new ArrayList<>();
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                processAccel(event);break;
            case Sensor.TYPE_GYROSCOPE:
                processAccel(event);break;
            default:break;
        }
        if(isStep)invalid_pos(pos);
        return pos;
    }
}
