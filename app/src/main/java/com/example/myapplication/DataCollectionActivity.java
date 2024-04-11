package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DataCollectionActivity extends AppCompatActivity implements SensorEventListener {
    private final StringBuilder sensorDataBuilder = new StringBuilder();
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;
    private DataCollectView dataCollectView;
    private boolean isCollectingData = false;
    private long initialTimestamp = 0;
    private long initialTimestampfile = 0;
    private String currentSessionFileName;
    private LineChart trajectoryView;
    private ViewPager viewPager;
    private ImageButton expandButton;
    private LinearLayout Btn_container;
    private boolean isExpanded = false;

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
        if (trajectory.size() != 0) {
            Toast.makeText(this, "解算成功！", Toast.LENGTH_SHORT).show();
            drawMap(trajectory);
        }
    }

    private void mapInitial() {
        trajectoryView.setBackgroundColor(Color.WHITE);
        trajectoryView.setDragEnabled(true);
        trajectoryView.getLegend().setEnabled(false);
        trajectoryView.getDescription().setEnabled(false);
        trajectoryView.setScaleEnabled(true);
        trajectoryView.setTouchEnabled(true);
        trajectoryView.setExtraOffsets(15f, 15f, 15f, 20f);

        XAxis xAxis = trajectoryView.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);

        YAxis leftYAxis = trajectoryView.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setInverted(true);
        leftYAxis.setGridColor(Color.LTGRAY);
        YAxis rightYAxis = trajectoryView.getAxisRight();
        rightYAxis.setEnabled(false);

        ArrayList<Entry> entries_start = new ArrayList<>();
        entries_start.add(new Entry(0, 0));

        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 0));

        LineDataSet dataSetStart = new LineDataSet(entries_start, "Start");
        dataSetStart.setCircleColor(Color.parseColor("#03A9F4"));
        dataSetStart.setDrawCircles(true);
        dataSetStart.setCircleRadius(6);
        dataSetStart.setDrawValues(false);
        dataSetStart.setLineWidth(0);

        LineDataSet dataSet = new LineDataSet(entries, "trajectory");
        dataSet.setColor(Color.parseColor("#EEDC82"));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(4);

        LineData lineData = new LineData(dataSetStart, dataSet);
        trajectoryView.setData(lineData);
    }

    private void drawMap(List<double[]> pos) {
        LineData lineData = trajectoryView.getLineData();
        for (int i = 0; i < pos.size(); i++) {
            double[] p = pos.get(i);
            lineData.addEntry(new Entry((float) p[0], (float) p[1]), 1);
        }
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry((float) pos.get(pos.size() - 1)[0], (float) pos.get(pos.size() - 1)[1]));
        LineDataSet dataSetStop = new LineDataSet(entries, "Stop");
        dataSetStop.setCircleColor(Color.parseColor("#EC4F44"));
        dataSetStop.setDrawCircles(true);
        dataSetStop.setCircleRadius(6);
        dataSetStop.setDrawValues(false);
        dataSetStop.setLineWidth(0);
        lineData.addDataSet(dataSetStop);
        lineData.notifyDataChanged();
        trajectoryView.notifyDataSetChanged();
        trajectoryView.invalidate();
    }
    private void saveChartAsImage(LineChart chart) {
        // 保存图表为图片
        chart.saveToGallery("chart", 600); // 文件名为 "chart"，质量为 100
        Toast.makeText(this, "Chart saved as image", Toast.LENGTH_SHORT).show();
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

        dataCollectView = findViewById(R.id.data_collect_view);
        trajectoryView = findViewById(R.id.trajectoryView);
        trajectoryView.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartLongPressed(MotionEvent me) {
                // 弹出确认对话框
                showConfirmationDialog();
            }

            // 其余方法保持空白
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}
            @Override
            public void onChartDoubleTapped(MotionEvent me) {}
            @Override
            public void onChartSingleTapped(MotionEvent me) {}
            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}
            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {}
        });
        viewPager = findViewById(R.id.viewpager);
        ArrayList<View> view_array = new ArrayList<>();
        view_array.add(dataCollectView);
        view_array.add(trajectoryView);
        MyPagerAdapter adapter = new MyPagerAdapter(view_array);
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

        expandButton = findViewById(R.id.expandButton);
        Btn_container = findViewById(R.id.buttonsContainer);
        expandButton.setOnClickListener(v -> Btn_move());
        mapInitial();
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存");
        builder.setMessage("确认保存？");
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户点击确认按钮时保存图片
                saveChartAsImage();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户点击取消按钮时关闭对话框
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void saveChartAsImage() {
        // 生成带时间戳的文件名
        String fileName = "chart_" + System.currentTimeMillis();
        // 保存图表为图片，质量设置为 600
        trajectoryView.saveToGallery(fileName, 600);
        Toast.makeText(this, "图片已保存", Toast.LENGTH_SHORT).show();
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
            if (initialTimestamp == 0) {
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

    private void Btn_move() {
        if (isExpanded) {
            // 向上移动 100dp
            animateLayout(Btn_container, -100);
            expandButton.setImageResource(R.drawable.ic_expand_down);
        } else {
            // 向下移动 100dp
            animateLayout(Btn_container, 100);
            expandButton.setImageResource(R.drawable.ic_expand_up);
        }

        // 切换状态
        isExpanded = !isExpanded;
    }

    private void animateLayout(@NonNull final View view, float targetY) {
        float density = getResources().getDisplayMetrics().density;
        float targetYDp = targetY * density;

        // 获取视图的初始位置
        final float startY = view.getY();
        final float endY = startY + targetYDp;

        // 创建一个 ValueAnimator 对象，用于执行动画
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

        animator.setDuration(300); // 设置动画时长为 600 毫秒
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // 获取动画的当前进度（取值范围 0~1）
                float progress = (float) animation.getAnimatedValue();
                // 计算当前位置
                float currentY = startY + progress * (endY - startY);
                // 更新视图的位置
                view.setY(currentY);
            }
        });
        // 启动动画
        animator.start();
    }

}