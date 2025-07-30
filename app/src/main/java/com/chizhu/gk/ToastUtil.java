package com.chizhu.gk;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ToastUtil {
    public static void showCustomToast(Context context, String message) {
        // 加载布局
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        // 设置文本
        TextView textView = layout.findViewById(R.id.toast_text);
        textView.setText(message);

        // 创建Toast实例
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
        toast.setView(layout);
        toast.show();
    }
}