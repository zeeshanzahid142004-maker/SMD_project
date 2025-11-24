package com.example.learnify;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class DialogHelper {

    /**
     * Create a beautiful Material Design loading dialog
     */
    public static Dialog createLoadingDialog(Context context, String message) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);

        TextView tvTitle = view.findViewById(R.id.tv_loading_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_loading_subtitle);

        if (message != null) {
            if (message.contains("Generating")) {
                tvTitle.setText("Getting Ready...");
                tvSubtitle.setText("Analyzing content and crafting questions just for you.");
            } else if (message.contains("Saving")) {
                tvTitle.setText("Saving Quiz...");
                tvSubtitle.setText("Storing your quiz for later access.");
            } else {
                tvTitle.setText(message);
                tvSubtitle.setVisibility(View.GONE);
            }
        }

        dialog.setContentView(view);

        // Make background transparent so card shows properly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    /**
     * Show a simple alert dialog with custom styling
     */
    public static void showAlert(Context context, String title, String message,
                                 String positiveButton, Runnable onPositive) {
        new androidx.appcompat.app.AlertDialog.Builder(context, R.style.MaterialAlertDialog_Rounded)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    if (onPositive != null) onPositive.run();
                })
                .show();
    }

    /**
     * Show a confirmation dialog with two options
     */
    public static void showConfirmation(Context context, String title, String message,
                                        String positiveButton, Runnable onPositive,
                                        String negativeButton, Runnable onNegative) {
        new androidx.appcompat.app.AlertDialog.Builder(context, R.style.MaterialAlertDialog_Rounded)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    if (onPositive != null) onPositive.run();
                })
                .setNegativeButton(negativeButton, (dialog, which) -> {
                    if (onNegative != null) onNegative.run();
                })
                .show();
    }
}