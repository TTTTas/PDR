package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

public class DataCollectionActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;
    private TextView accelerometerData, gyroscopeData, magnetometerData;
    private boolean isCollectingData = false;
    private long initialTimestamp = 0;
    private long initialTimestampfile = 0;
    private TextView collectionTimeTextView;
    private String currentSessionFileName;
    private StringBuilder sensorDataBuilder = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化UI组件
        accelerometerData = findViewById(R.id.accelerometerData);
        gyroscopeData = findViewById(R.id.gyroscopeData);
        magnetometerData = findViewById(R.id.magnetometerData);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        collectionTimeTextView = findViewById(R.id.collectionTime);

        // 获取传感器服务
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 设置按钮监听器
        startButton.setOnClickListener(v -> startDataCollection());
        stopButton.setOnClickListener(v -> stopDataCollection());
    }

    private void startDataCollection() {
        if (!isCollectingData) {
            // 注册传感器监听器
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            // 为新的采集会话创建文件名
            initialTimestampfile = System.currentTimeMillis();
            currentSessionFileName = "SensorData_" + initialTimestampfile + ".txt";
            isCollectingData = true;
        }
    }

    private void stopDataCollection() {
        if (isCollectingData) {
            // 注销传感器监听器
            sensorManager.unregisterListener(this);
            // 将累积的数据写入文件
            FileHelper.writeToFile(this, currentSessionFileName, sensorDataBuilder.toString());
            // 清空StringBuilder以释放内存
            sensorDataBuilder.setLength(0);
            isCollectingData = false;
            initialTimestamp = 0;
            // 显示数据文件保存成功的消息提示
            Toast.makeText(this, "数据文件保存成功！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isCollectingData) {
            // 如果是第一次收集数据，设置初始时间戳
            if (initialTimestamp == 0 && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                initialTimestamp = event.timestamp;
            }

            // 计算相对时间戳（单位：秒）
            float relativeTimestamp = (event.timestamp - initialTimestamp) / 1_000_000_000.0f;

            // 将传感器数据写入文件
            String sensorDataLine = formatSensorDataLine(event.sensor.getType(), relativeTimestamp, event.values[0], event.values[1], event.values[2]);
            // 将传感器数据添加到StringBuilder
            sensorDataBuilder.append(sensorDataLine).append("\n");

            // 格式化传感器数据字符串
            String dataString = String.format("%s: %.3f s  %.4f %.4f %.4f",
                    getSensorString(event.sensor.getType()),
                    relativeTimestamp, event.values[0], event.values[1], event.values[2]);

            // 根据传感器类型更新UI
            switch (event.sensor.getType()) {
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
            collectionTimeTextView.setText(String.format("已采集时间: %.3f秒", relativeTimestamp));

        }
    }

    private String getSensorString(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "acc(m/s2)";
            case Sensor.TYPE_GYROSCOPE:
                return "gyro(rad/s)";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "mag(uT)";
            default:
                return "";
        }
    }

    // 格式化传感器数据行的方法
    private String formatSensorDataLine(int sensorType, float timestamp, float x, float y, float z) {
        int sensorIdentifier;
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorIdentifier = 1;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorIdentifier = 2;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorIdentifier = 3;
                break;
            default:
                sensorIdentifier = -1; // Unknown sensor type
        }
        return String.format("%d %f %f %f %f", sensorIdentifier, timestamp, x, y, z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 在这里处理传感器精度变化
    }

}