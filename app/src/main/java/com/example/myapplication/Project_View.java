package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Gravity;
import android.view.MotionEvent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Project_View extends AppCompatTextView {
    private String project_Name;
    private String data_Path;
    private String file_Path;
    private String cover_Path;
    private OnDeleteListener onDeleteListener;
    private OnClickListener onClickListener;

    public Project_View(Context context) {
        super(context);
        init("default");
    }

    public Project_View(Context context, AttributeSet attrs) {
        super(context, attrs);
        init("default");
    }

    public Project_View(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init("default");
    }

    public Project_View(Context context, String projectName) {
        super(context);
        init(projectName);
    }

    private void init(String projectName) {
        this.setClickable(true);
        project_Name = projectName;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        data_Path = currentDateAndTime + "_" + project_Name + ".txt";
        String filename="Project_"+project_Name + "_" + currentDateAndTime+".txt";
        file_Path = getContext().getFilesDir() + "/" + filename;
        cover_Path = currentDateAndTime + "_" + project_Name + ".png";
        writeToFile(getContext().getFilesDir(), filename);
        // 创建 SpannableString
        SpannableString spannableString = new SpannableString("   " + project_Name);

        // 获取图片资源
        Drawable drawable = getResources().getDrawable(R.drawable.initial_pic);
        int newWidth = (int) (drawable.getIntrinsicWidth() * (float) getResources().getDimensionPixelSize(R.dimen.project_view_height) / drawable.getIntrinsicHeight());
        drawable.setBounds(0, 0, newWidth, getResources().getDimensionPixelSize(R.dimen.project_view_height));
        // 创建 ImageSpan
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);

        // 将图片插入到 SpannableString 的开头
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new LeadingMarginSpan.Standard((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        // 设置 TextView 的文本
        setText(spannableString);
        // 设置字体大小为 25sp
        setTextSize(25);
        // 设置字体颜色为黑色
        setTextColor(Color.BLACK);
        // 设置文本框下横线颜色为灰色
        setBackgroundResource(R.drawable.project_view_background);
        // 设置文字和图片居中显示
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START); // 文字和图片居中对齐

        // 设置宽度为 match_parent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.project_view_height) // 高度为 36dp
        );
        setLayoutParams(params);
        // 设置长按监听器
        setLongClickListener();

        // 设置点击监听器
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 执行点击后的操作，比如打开项目详情页或者执行其他逻辑
                Toast.makeText(getContext(), "Project " + project_Name + " clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("Project_View", "onTouchEvent: ACTION_DOWN");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("Project_View", "onTouchEvent: ACTION_UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("Project_View", "onTouchEvent: ACTION_CANCEL");
                break;
        }
        return result;
    }

    private void writeToFile(File directory, String filename) {
        File file = new File(directory, filename);
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Project Name: " + project_Name + "\n");
            writer.append("Data Path: " + data_Path + "\n");
            writer.append("Cover Path: " + cover_Path);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     private void setLongClickListener() {
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹出删除对话框
                showDeleteDialog();
                return true;
            }
        });
    }

    private void showDeleteDialog() {
        // 在这里实现弹出删除对话框的逻辑
        // 提示用户是否确认删除
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("确认删除该工程？")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 调用删除方法
                        delete();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void delete() {
        // 释放图片资源
        recycleDrawable();
        // 如果有设置删除监听器，则回调删除方法
        if (onDeleteListener != null) {
            onDeleteListener.onDelete(this);
        }
    }

    private void recycleDrawable() {
        Drawable[] drawables = getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setCallback(null);
            }
        }
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        onDeleteListener = listener;
    }

    public interface OnDeleteListener {
        void onDelete(Project_View projectView);
    }

    public String getProjectName() {
        return project_Name;
    }

    public String getDataPath() {
        return data_Path;
    }

    public String getFilePath() {
        return file_Path;
    }

    public String getCoverPath() { return cover_Path; }
}
