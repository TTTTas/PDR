package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import com.example.myapplication.R;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class Load_Page extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
    }
}
