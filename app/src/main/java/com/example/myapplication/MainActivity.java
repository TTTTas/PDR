package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.widget.Button;

import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private boolean writeToSameFile = false;
    private String currentFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // 寻找并匹配按钮
        Button startStopButton = findViewById(R.id.startStopButton);

        // 设置按钮点击事件监听
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartStopButtonClick();
            }
        });

        // 生成当前文件名
        currentFileName = "sensor_data_" + System.currentTimeMillis() + ".txt";

        // 获取所有传感器列表
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void onStartStopButtonClick() {
        Button startStopButton = findViewById(R.id.startStopButton);

        if (!writeToSameFile) {
            writeToSameFile = true;
            startStopButton.setText("Stop Collecting Data");
        } else {
            writeToSameFile = false;
            startStopButton.setText("Start Collecting Data");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = -1;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorType = 2;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorType = 1;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorType = 3;
        }

        if (sensorType != -1 && writeToSameFile) {
            long timestamp = event.timestamp;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 构建要写入文件的数据字符串
            String data = sensorType + ", " + timestamp + ", " + x + ", " + y + ", " + z;

            // 将数据写入文件
            FileHelper.writeToFile(this, currentFileName, data);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 精度变化时的处理
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}
