package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RealTimePDRActivity extends AppCompatActivity {
    private View grayBar;
    private TextView part3;
    private float startY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_pdractivity);

        RelativeLayout part2=findViewById(R.id.part2Container);
        FrameLayout map_container=findViewById(R.id.mapContainer);
        grayBar = findViewById(R.id.grayBar);
        part3 = findViewById(R.id.part3);
        // 设置灰色bar的触摸监听
        grayBar.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = event.getRawY() - startY;
                    if (deltaY<0)
                    {
                        part3.setVisibility(View.VISIBLE);
                        TranslateAnimation animation = new TranslateAnimation(0, 0, part3.getHeight(), 0);
                        animation.setDuration(300); // 设置动画持续时间为 300 毫秒
                        part2.startAnimation(animation);
                        map_container.startAnimation(animation);
                    }
                    else if(deltaY>0)
                    {
                        part3.setVisibility(View.GONE);
                        TranslateAnimation animation = new TranslateAnimation(0, 0, -part3.getHeight(), 0);
                        animation.setDuration(300); // 设置动画持续时间为 300 毫秒
                        part2.startAnimation(animation);
                        map_container.startAnimation(animation);
                        startY = event.getRawY();
                    }
                    break;
            }
            return true;
        });

    }
}


