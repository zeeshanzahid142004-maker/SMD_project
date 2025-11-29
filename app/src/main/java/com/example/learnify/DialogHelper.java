package com.example.learnify;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.List;

public class DialogHelper {

    /**
     * Callback interface for theme selection
     */
    public interface ThemeSelectionListener {
        void onThemeSelected(int mode);
    }

    /**
     * Callback interface for feedback dialog action
     */
    public interface FeedbackDialogListener {
        void onAction();
    }

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
                tvSubtitle.setVisibility(View.VISIBLE);
            } else if (message.contains("Saving")) {
                tvTitle.setText("Saving Quiz...");
                tvSubtitle.setText("Storing your quiz for later access.");
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setText(message);
                tvSubtitle.setVisibility(View.GONE);
            }
        }

        dialog.setContentView(view);

        // Make background transparent so card shows properly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Fix layout width
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    /**
     * Create an animated loading dialog with Lottie animation
     */
    public static Dialog createAnimatedLoadingDialog(Context context, String title, String subtitle) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading_animated, null);

        LottieAnimationView lottieView = view.findViewById(R.id.lottie_loading);
        TextView tvTitle = view.findViewById(R.id.tv_loading_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_loading_subtitle);

        if (title != null) {
            tvTitle.setText(title);
        }

        if (subtitle != null && !subtitle.isEmpty()) {
            tvSubtitle.setText(subtitle);
            tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }

        // Start animation
        lottieView.playAnimation();

        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    /**
     * Create a success feedback dialog with Lottie animation
     */
    public static Dialog createSuccessDialog(Context context, String title, String message, 
                                             String buttonText, FeedbackDialogListener listener) {
        return createFeedbackDialog(context, title, message, buttonText, R.raw.success_check, listener);
    }

    /**
     * Create an error feedback dialog with Lottie animation
     */
    public static Dialog createErrorDialog(Context context, String title, String message, 
                                           String buttonText, FeedbackDialogListener listener) {
        return createFeedbackDialog(context, title, message, buttonText, R.raw.error_alert, listener);
    }

    /**
     * Create a generic feedback dialog with custom Lottie animation
     */
    private static Dialog createFeedbackDialog(Context context, String title, String message,
                                               String buttonText, int animationRes, 
                                               FeedbackDialogListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_feedback, null);

        LottieAnimationView lottieView = view.findViewById(R.id.lottie_feedback);
        TextView tvTitle = view.findViewById(R.id.tv_feedback_title);
        TextView tvMessage = view.findViewById(R.id.tv_feedback_message);
        MaterialButton btnAction = view.findViewById(R.id.btn_feedback_action);

        // Set animation
        lottieView.setAnimation(animationRes);
        lottieView.playAnimation();

        // Set content
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnAction.setText(buttonText != null ? buttonText : "OK");

        // Set button listener
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onAction();
            }
        });

        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    /**
     * Show the custom Theme Selection Dialog
     */
    public static void showThemeSelectionDialog(Context context, int currentMode, ThemeSelectionListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        // Inflate the custom layout
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_theme_selection, null);
        dialog.setContentView(view);

        // Transparent background for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RadioGroup radioGroup = view.findViewById(R.id.theme_radio_group);
        RadioButton optLight = view.findViewById(R.id.opt_light);
        RadioButton optDark = view.findViewById(R.id.opt_dark);
        RadioButton optSystem = view.findViewById(R.id.opt_system);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        // Pre-select the current mode
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            optLight.setChecked(true);
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            optDark.setChecked(true);
        } else {
            optSystem.setChecked(true);
        }

        // Handle Selection
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            if (checkedId == R.id.opt_light) {
                selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.opt_dark) {
                selectedMode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            // Add a tiny delay so the user sees the click animation
            int finalMode = selectedMode;
            view.postDelayed(() -> {
                dialog.dismiss();
                if (listener != null) listener.onThemeSelected(finalMode);
            }, 200);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
    /**
     * Callback interface for image source selection
     */
    public interface ImageSourceCallback {
        void onCameraSelected();
        void onGallerySelected();
    }

    /**
     * Show Camera/Gallery picker dialog
     */
    public static void showImageSourceDialog(Context context, ImageSourceCallback callback) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_source, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow(). setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams. WRAP_CONTENT);
        }

        View btnClose = view.findViewById(R.id.btn_close);
        View optionCamera = view.findViewById(R.id.option_camera);
        View optionGallery = view. findViewById(R.id.option_gallery);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        optionCamera.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onCameraSelected();
        });

        optionGallery.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onGallerySelected();
        });

        dialog.show();
    }
    public interface LanguageSelectionListener {
        void onLanguageSelected(LanguageManager.Language language);
    }

    /**
     * Show the Custom Language Selection Dialog
     */
    public static void showConfirmation(Context context, String title, String message,
                                        String positiveButton, Runnable onPositive,
                                        String negativeButton, Runnable onNegative) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_confirmation, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = view.findViewById(R.id.dialog_title);
        TextView tvMessage = view.findViewById(R.id.dialog_message);
        Button btnPos = view.findViewById(R.id.btn_positive);
        Button btnNeg = view.findViewById(R.id.btn_negative);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPos.setText(positiveButton);
        btnNeg.setText(negativeButton);

        btnPos.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.run();
        });

        btnNeg.setOnClickListener(v -> {
            dialog.dismiss();
            if (onNegative != null) onNegative.run();
        });

        dialog.show();
    }

    public static void showLanguageSelectionDialog(Context context,
                                                   List<LanguageManager.Language> languages,
                                                   String currentCode,
                                                   LanguageSelectionListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_language_selection, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RadioGroup radioGroup = view.findViewById(R.id.language_radio_group);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        // Colors for programmatic views
        int purpleColor = ContextCompat.getColor(context, R.color.figma_purple_main);
        int textColor = ContextCompat.getColor(context, R.color.text_primary_dark);
        int dividerColor = Color.parseColor("#F0F0F0");

        // Dynamically add radio buttons
        for (int i = 0; i < languages.size(); i++) {
            LanguageManager.Language lang = languages.get(i);

            MaterialRadioButton rb = new MaterialRadioButton(context);
            rb.setText(lang.nativeName + " (" + lang.name + ")");
            rb.setTextSize(16);
            rb.setTextColor(textColor);
            rb.setPadding(32, 32, 32, 32); // Padding for touch target
            rb.setButtonTintList(ColorStateList.valueOf(purpleColor));
            rb.setId(i); // Use index as ID for simplicity

            // Check if this is the current language
            if (lang.code.equals(currentCode)) {
                rb.setChecked(true);
            }

            radioGroup.addView(rb);

            // Add divider (except for last item)
            if (i < languages.size() - 1) {
                View divider = new View(context);
                divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)); // 1dp height approx
                divider.setBackgroundColor(dividerColor);
                radioGroup.addView(divider);
            }
        }

        // Handle Selection
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            LanguageManager.Language selectedLang = languages.get(checkedId);

            // Tiny delay for visual feedback
            view.postDelayed(() -> {
                dialog.dismiss();
                if (listener != null) listener.onLanguageSelected(selectedLang);
            }, 200);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Callback interface for image source selection
     */

    public static Dialog createImageSourceDialog(Context context, ImageSourceCallback callback) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_image_source, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Close button
        View btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Camera option
        View optionCamera = view.findViewById(R.id.option_camera);
        optionCamera.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) {
                callback.onCameraSelected();
            }
        });

        // Gallery option
        View optionGallery = view.findViewById(R.id.option_gallery);
        optionGallery.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) {
                callback.onGallerySelected();
            }
        });

        return dialog;
    }

}