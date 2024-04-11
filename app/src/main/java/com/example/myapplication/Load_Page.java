package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.annotation.SuppressLint;
import com.example.myapplication.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class Load_Page extends AppCompatActivity {
    private LinearLayout projectContainer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        projectContainer=findViewById(R.id.proj_container);
        FloatingActionButton createProjectButton = findViewById(R.id.btn_add_proj);
        createProjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建弹出窗口
                AlertDialog.Builder builder = new AlertDialog.Builder(Load_Page.this);
                View dialogView = getLayoutInflater().inflate(R.layout.create_project_dialog, null);
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                // 获取弹出窗口中的视图和按钮
                EditText projectNameEditText = dialogView.findViewById(R.id.projectNameEditText);
                Button confirmButton = dialogView.findViewById(R.id.confirmButton);
                Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                // 确定按钮的点击事件
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String projectName = projectNameEditText.getText().toString().trim();
                        if (!projectName.isEmpty()) {
                            createProject(projectName);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(Load_Page.this, "Project name cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // 取消按钮的点击事件
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                // 显示弹出窗口
                dialog.show();
            }
        });
    }

    private void createProject(String projectName) {
        final Project_View projectView = new Project_View(Load_Page.this, projectName);
        // 设置 LinearLayout.LayoutParams 来控制子视图的布局
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // 设置子视图与其他视图的间距
        params.setMargins(0, 0, 0, 10); // 例如，底部间距设置为 20 像素

        projectView.setLayoutParams(params);

        projectView.setOnDeleteListener(new Project_View.OnDeleteListener() {
            @Override
            public void onDelete(Project_View projectView) {
                deleteProject(projectView, Load_Page.this);
            }
        });

        projectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取项目的数据路径和封面路径
                String dataPath = projectView.getDataPath();
                String coverPath = projectView.getCoverPath();

                // 创建 Intent 并传递数据路径和封面路径到 DataCollectionActivity
                Intent intent = new Intent(Load_Page.this, DataCollectionActivity.class);
                intent.putExtra("data_path", dataPath);
                intent.putExtra("cover_path", coverPath);
                startActivity(intent);
            }
        });

        projectContainer.addView(projectView);

    }

    private void deleteProject(Project_View projectView, Context context) {
        projectContainer.removeView(projectView);
        String filePath = projectView.getFilePath();
        if (filePath != null) {
            File file = new File(filePath);
            Log.d("DeleteProject", "File path: " + filePath); // 添加调试语句
            if (file.exists()) {
                if (file.delete()) {
                    Toast.makeText(context, "文件删除成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "文件删除失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
