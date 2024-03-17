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
import android.content.Intent;
import java.util.List;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置标题栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 数据采集按钮
        Button dataCollectionButton = findViewById(R.id.button_data_collection);
        dataCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动数据采集Activity
                Intent intent = new Intent(MainActivity.this, DataCollectionActivity.class);
                startActivity(intent);
            }
        });

        // 实时PDR按钮
        Button realTimePdrButton = findViewById(R.id.button_real_time_pdr);
        realTimePdrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动实时PDR Activity
                Intent intent = new Intent(MainActivity.this, RealTimePDRActivity.class);
                startActivity(intent);
            }
        });


    }


}
