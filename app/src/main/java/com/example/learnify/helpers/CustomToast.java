package com.example.learnify.helpers;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.learnify.R;

public class CustomToast {

    public enum Type {
        SUCCESS,
        ERROR,
        INFO,
        WARNING
    }

    /**
     * Show a success toast and return the instance
     */
    public static Toast success(Context context, String message) {
        return show(context, message, Type.SUCCESS);
    }

    /**
     * Show an error toast and return the instance
     */
    public static Toast error(Context context, String message) {
        return show(context, message, Type.ERROR);
    }

    /**
     * Show an info toast and return the instance
     */
    public static Toast info(Context context, String message) {
        return show(context, message, Type.INFO);
    }

    /**
     * Show a warning toast and return the instance
     */
    public static Toast warning(Context context, String message) {
        return show(context, message, Type.WARNING);
    }

    /**
     * Show a custom toast, display it, and return the object
     */
    public static Toast show(Context context, String message, Type type) {
        if (context == null || message == null) return null;

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_custom, null);

        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);

        text.setText(message);

        // Set background and icon based on type
        switch (type) {
            case SUCCESS:
                layout.setBackgroundResource(R.drawable.bg_toast_success);
                icon.setImageResource(R.drawable.ic_check_circle);
                break;
            case ERROR:
                layout.setBackgroundResource(R.drawable.bg_toast_error);
                icon.setImageResource(R.drawable.ic_error_circle);
                break;
            case INFO:
                layout.setBackgroundResource(R.drawable.bg_toast_info);
                icon.setImageResource(R.drawable.ic_info);
                break;
            case WARNING:
                layout.setBackgroundResource(R.drawable.bg_toast_warning);
                icon.setImageResource(R.drawable.ic_warning);
                text.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark));
                icon.setColorFilter(ContextCompat.getColor(context, R.color.text_primary_dark));
                break;
        }

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();

        return toast; // ✅ NOW RETURNS THE OBJECT
    }
}