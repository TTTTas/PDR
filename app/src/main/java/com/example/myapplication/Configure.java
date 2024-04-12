package com.example.myapplication;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class Configure extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // 创建对话框并设置标题
        Dialog dialog = new Dialog(requireActivity());
        dialog.setTitle("Configure");

        // 设置对话框的内容视图
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.configure_layout, null);
        dialog.setContentView(contentView);

        return dialog;
    }
}
