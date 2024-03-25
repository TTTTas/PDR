package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class TrajectoryView extends View {
    private Paint paint; // 用于绘制的Paint对象
    private Paint borderPaint; // 用于绘制边框的Paint对象
    private List<double[]> trajectory; // 存储轨迹数据的列表

    public TrajectoryView(Context context) {
        super(context);
        init();
    }

    public TrajectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLUE); // 设置绘制颜色
        paint.setStrokeWidth(5); // 设置线条宽度
        paint.setStyle(Paint.Style.STROKE); // 设置样式为描边

        borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK); // 设置边框颜色
        borderPaint.setStyle(Paint.Style.STROKE); // 设置样式为描边
        borderPaint.setStrokeWidth(4); // 设置边框宽度
    }

    public void setTrajectory(List<double[]> trajectory) {
        this.trajectory = trajectory;
        invalidate(); // 请求重绘视图
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制边框
        RectF borderRect = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawRect(borderRect, borderPaint);

        if (trajectory == null || trajectory.isEmpty()) {
            return; // 如果轨迹数据为空，则不绘制
        }

        // 计算轨迹数据的最大和最小坐标值
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (double[] point : trajectory) {
            minX = Math.min(minX, point[0]);
            maxX = Math.max(maxX, point[0]);
            minY = Math.min(minY, point[1]);
            maxY = Math.max(maxY, point[1]);
        }

        // 计算轨迹数据的范围
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;

        // 计算缩放比例和偏移量
        double scaleX = getWidth() / rangeX;
        double scaleY = getHeight() / rangeY;
        double offsetX = minX;
        double offsetY = minY;

        // 绘制轨迹
        for (int i = 0; i < trajectory.size() - 1; i++) {
            double[] start = trajectory.get(i);
            double[] end = trajectory.get(i + 1);
            float startX = (float) ((start[0] - offsetX) * scaleX);
            float startY = (float) ((start[1] - offsetY) * scaleY);
            float endX = (float) ((end[0] - offsetX) * scaleX);
            float endY = (float) ((end[1] - offsetY) * scaleY);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }
}