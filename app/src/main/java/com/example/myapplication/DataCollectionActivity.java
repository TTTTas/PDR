package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DataCollectionActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;
    private DataCollectView dataCollectView;
    private boolean isCollectingData = false;
    private long initialTimestamp = 0;
    private long initialTimestampfile = 0;
    private String currentSessionFileName;
    private final StringBuilder sensorDataBuilder = new StringBuilder();
    private TrajectoryMap trajectoryView;
    private ViewPager viewPager;

    private void processPDR(String filename) {
        List<String> sensorDataLines = new ArrayList<>();
        // 获取外部存储的公共文档目录
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);

        try {
            // 打开文件输入流
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            String line;
            while ((line = reader.readLine()) != null) {
                sensorDataLines.add(line);
            }

            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "读取数据文件失败！", Toast.LENGTH_SHORT).show();
            return;
        }


        // 创建PDR处理器实例并处理数据
        PDRProcessor pdrProcessor = new PDRProcessor();
        List<double[]> trajectory = pdrProcessor.processSensorData(sensorDataLines);

        // 将轨迹数据传递给TrajectoryView进行绘制
        runOnUiThread(() -> {
            if (trajectoryView != null) {
                trajectoryView.drawMap(trajectory);
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化UI组件

        ImageButton startButton = findViewById(R.id.StartButton);
        ImageButton pauseButton = findViewById(R.id.PauseButton);
        ImageButton stopButton = findViewById(R.id.StopButton);
        ImageButton settingButton = findViewById(R.id.SettingButton);
        dataCollectView = findViewById(R.id.datacollectview);
        trajectoryView = findViewById(R.id.trajectoryView);
        viewPager = findViewById(R.id.viewpager);
        ArrayList<View> viewarray = new ArrayList<>();
        viewarray.add(dataCollectView);
        viewarray.add(trajectoryView);
        MyPagerAdapter adapter = new MyPagerAdapter(viewarray);
        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);

        // 获取传感器服务
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 设置按钮监听器
        startButton.setOnClickListener(v -> startDataCollection());
        stopButton.setOnClickListener(v -> stopDataCollection());

        // 事后PDR按钮
        ImageButton processPDRButton = findViewById(R.id.processPDRButton);
        processPDRButton.setOnClickListener(v -> processPDR(currentSessionFileName));


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
            String dataString = String.format("%s:\n %.4f %.4f %.4f",
                    getSensorString(event.sensor.getType()),
                    event.values[0], event.values[1], event.values[2]);

            dataCollectView.update(relativeTimestamp, event.values[0], event.values[1], event.values[2], event.sensor.getType(), dataString);


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