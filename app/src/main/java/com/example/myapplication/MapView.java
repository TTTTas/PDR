package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MapView extends View {

    private Paint start_point_paint;//开始圆点
    private Paint end_point_paint;//结束圆点
    private Paint line_paint;//线
    private Paint text_paint;//文字
    private Path path;//轨迹
    private List<XY> xyList = new ArrayList<>();//绘制点集合

    private int view_width;//view的宽
    private int view_height;//view的高

    private float x_point_left = 0;//轨迹最左（西）边的X点
    private float x_point_right = 0;//轨迹最右（东）边的X点
    private float y_point_top = 0;//轨迹最顶（北）边的Y点
    private float y_point_bottom = 0;//轨迹最底（南）边的Y点

    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        start_point_paint = new Paint();

        end_point_paint = new Paint();

        line_paint = new Paint();

        text_paint = new Paint();

        path = new Path();
    }

    //绘制前重置一下避免一致绘制缓存过大出现卡顿现象
    private void reset() {
        start_point_paint.reset();
        start_point_paint.setAntiAlias(true);
        start_point_paint.setDither(true);
        start_point_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        start_point_paint.setColor(Color.parseColor("#139DFF"));
        start_point_paint.setStrokeWidth(15.0f);

        end_point_paint.reset();
        end_point_paint.setAntiAlias(true);
        end_point_paint.setDither(true);
        end_point_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        end_point_paint.setColor(Color.RED);
        end_point_paint.setStrokeWidth(15.0f);

        line_paint.reset();
        line_paint.setAntiAlias(true);
        line_paint.setDither(true);
        line_paint.setStyle(Paint.Style.STROKE);
        line_paint.setColor(Color.GREEN);
        line_paint.setStrokeWidth(5.0f);
        line_paint.setStrokeJoin(Paint.Join.ROUND);//设置线段连接处为圆角

        text_paint.reset();
        text_paint.setAntiAlias(true);
        text_paint.setDither(true);
        text_paint.setColor(Color.BLACK);
        text_paint.setTextSize(20.0f);

        path.reset();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i("view", "onMeasure");
        int width_size = MeasureSpec.getSize(widthMeasureSpec);
        int width_mode = MeasureSpec.getMode(widthMeasureSpec);
        int height_size = MeasureSpec.getSize(heightMeasureSpec);
        int height_mode = MeasureSpec.getMode(heightMeasureSpec);

        if (width_mode == MeasureSpec.EXACTLY) {
            view_width = width_size;
        } else if (width_mode == MeasureSpec.AT_MOST) {
            view_width = getPaddingStart() + getPaddingEnd();
        }
        if (height_mode == MeasureSpec.EXACTLY) {
            view_height = height_size;
        } else if (height_mode == MeasureSpec.AT_MOST) {
            view_height = getPaddingTop() + getPaddingBottom();
        }
        Log.d("view", "view_width:" + view_width + " *** view_height:" + view_height);
        setMeasuredDimension(view_width, view_height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.i("view", "onLayout");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i("view", "onDraw");
        Log.i("view", "xyList.size: " + xyList.size());
        reset();
        if (xyList.size() == 0) {
            return;
        }

        Log.d("view", "x_point_left: " + x_point_left);
        Log.d("view", "x_point_right: " + x_point_right);
        Log.d("view", "y_point_top: " + y_point_top);
        Log.d("view", "y_point_bottom: " + y_point_bottom);

        float scaleNumX = 300f;//X轴默认值缩放倍数
        float scaleNumY = 300f;//Y轴默认值缩放倍数

        float path_width = (x_point_right - x_point_left) * scaleNumX;//pat图形的宽
        float path_height = (y_point_bottom - y_point_top) * scaleNumY;//pat图形的高

        if (path_width == 0 && path_height == 0 || view_width == 0 && view_height == 0) {//正常数据计算的宽高是不为0的
            Log.e("view", "path_width: " + path_width + " **** path_height: " + path_height);
            return;//数据异常不在执行，否则下面while死循环
        }

        while (path_width > (float) view_width) {
            scaleNumX -= 0.01f;
            path_width = (x_point_right - x_point_left) * scaleNumX;
        }
        while (path_width < (float) view_width) {
            scaleNumX += 0.01f;
            path_width = (x_point_right - x_point_left) * scaleNumX;
        }
        while (path_height > (float) view_height) {
            scaleNumY -= 0.01f;
            path_height = (y_point_bottom - y_point_top) * scaleNumY;
        }
        while (path_height < (float) view_height) {
            scaleNumY += 0.01f;
            path_height = (y_point_bottom - y_point_top) * scaleNumY;
        }
        Log.i("view", "scaleNumX: " + scaleNumX + " **** scaleNumY: " + scaleNumY);
        Log.i("view", "view_width: " + view_width + " **** view_height: " + view_height);
        Log.i("view", "path_width: " + path_width + " **** path_height: " + path_height);
//		Log.d("view", "view_width / 2 = " + view_width / 2 + " **** view_height / 2 = " + view_height / 2);
//		Log.d("view", "path_width / 2 = " + path_width / 2 + " **** path_height / 2 = " + path_height / 2);

//		///////////缩放后可绘制的区域///////////
//		RectF rectF = new RectF(0,0, path_width, path_height);
//		//绘制上述矩形区域
//		canvas.drawRect(rectF, line_paint);
//		/////////////////////////////////////

        float left = x_point_left * scaleNumX;//缩放倍数后的最左边点
        float right = x_point_right * scaleNumX;//...最右边点
        float top = y_point_top * scaleNumY;//...最顶边点
        float bottom = y_point_bottom * scaleNumY;//...最底边点
        Log.d("view", "left: " + left);
        Log.d("view", "right: " + right);
        Log.d("view", "top: " + top);
        Log.d("view", "bottom: " + bottom);

        /****************** 计算整个path图形居中显示，开始点X，Y轴需要缩放、平移的位置 start *****************/
        //缩小比例使开始和结束点不挨着边界
        float sx = 0.7f;//X等比缩小30%
        float sy = 0.7f;//Y等比缩小30%
        float px = view_width / 2f;
        float py = view_height / 2f;
        Log.d("view", "px: " + px + ", py: " + py);
        //缩小canvas
        canvas.scale(sx, sy, px, py);

        //计算开始点需要平移的位置
        float onePointX = 0;//平移的第一个X点
        float onePointY = 0;//平移的第一个Y点
        if (left <= 0) {
            float leftTo0X = 0 - left;//最左边点到0X的距离
            onePointX = xyList.get(0).x * scaleNumX + leftTo0X;
        }

        if (right >= path_width) {
            float rightToPw = right - path_width;//最右边点到path宽度的距离
            onePointX = xyList.get(0).x * scaleNumX - rightToPw;
        }

        if (top <= 0) {
            float topTo0Y = 0 - top;//最顶边点到0Y的距离
            onePointY = xyList.get(0).y * scaleNumY + topTo0Y;
        }

        if (bottom >= path_height) {
            float bottomTo0Y = bottom - path_height;//最底边点到path高度的距离
            onePointY = xyList.get(0).y * scaleNumY - bottomTo0Y;
        }

        if (xyList.get(0).x * scaleNumX > 0) {
            onePointX -= xyList.get(0).x * scaleNumX;
        } else {
            onePointX += 0 - xyList.get(0).x * scaleNumX;
        }

        if (xyList.get(0).y * scaleNumY > 0) {
            onePointY -= xyList.get(0).y * scaleNumY;
        } else {
            onePointY += 0 - xyList.get(0).y * scaleNumY;
        }
        Log.i("view", "onePointX: " + onePointX + ", onePointY: " + onePointY);
        Log.i("view", "xyList.get(0).x * scaleNumX: " + (xyList.get(0).x * scaleNumX) + ", xyList.get(0).y * scaleNumX: " + (xyList.get(0).y * scaleNumY));
        Log.i("view", "xyList.get(0).x: " + (xyList.get(0).x) + ", xyList.get(0).y: " + (xyList.get(0).y));
        //平移到计算的显示位置
        canvas.translate(onePointX, onePointY);
        /****************** 计算整个path图形居中显示，开始点X，Y轴需要缩放、平移的位置 end *****************/

        //绘制开始圆点
        canvas.drawCircle(xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY, 1.0f, start_point_paint);
        //绘制结束圆点
        canvas.drawCircle(xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY, 1.0f, end_point_paint);
        //绘制起点文字
        canvas.drawText("起点", xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY, text_paint);
        //绘制终点文字
        canvas.drawText("终点", xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY, text_paint);
        //设置起点位置
        path.moveTo(xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY);
        //设置终点位置
        //path.setLastPoint(xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY);
        for (int i = 0; i < xyList.size(); i++) {
            //连接其他点
            path.lineTo(xyList.get(i).x * scaleNumX, xyList.get(i).y * scaleNumY);
        }
        //绘制轨迹
        canvas.drawPath(path, line_paint);
    }

    /**
     * 设置数据
     *
     * @param trajectory
     */
    public void setData(List<double[]> trajectory) {
        for(int i=0;i<trajectory.size();i++)
        {
            xyList.add(new XY((float)trajectory.get(i)[0],(float)trajectory.get(i)[1]));
        }
        invalidate();
    }


    public static class XY {
        float x;
        float y;

        public XY() {
        }

        public XY(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

}