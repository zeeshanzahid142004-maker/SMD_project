package com.example.learnify;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

/**
 * ImageTextExtractor - OCR utility class using Google ML Kit
 * 
 * Extracts text from images (photos of textbooks, notes, etc.) for quiz generation
 */
public class ImageTextExtractor {

    private static final String TAG = "ImageTextExtractor";
    private final Context context;
    private final TextRecognizer recognizer;

    public interface ExtractionCallback {
        void onSuccess(String extractedText);
        void onError(String error);
    }

    public ImageTextExtractor(Context context) {
        this.context = context;
        // Initialize ML Kit text recognizer with Latin script options
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    /**
     * Extract text from an image URI
     * @param imageUri URI of the image to process
     * @param callback Callback to receive extracted text or error
     */
    public void extractTextFromUri(Uri imageUri, ExtractionCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            processImage(image, callback);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create InputImage from URI", e);
            callback.onError("Failed to load image: " + e.getMessage());
        }
    }

    /**
     * Extract text from a Bitmap
     * @param bitmap Bitmap image to process
     * @param callback Callback to receive extracted text or error
     */
    public void extractTextFromBitmap(Bitmap bitmap, ExtractionCallback callback) {
        if (bitmap == null) {
            callback.onError("Bitmap is null");
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        processImage(image, callback);
    }

    /**
     * Process the InputImage with ML Kit Text Recognition
     */
    private void processImage(InputImage image, ExtractionCallback callback) {
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    String extractedText = text.getText();
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        callback.onError("No text found in image");
                    } else {
                        Log.d(TAG, "✅ Extracted " + extractedText.length() + " characters from image");
                        callback.onSuccess(extractedText);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Text recognition failed", e);
                    callback.onError("Text recognition failed: " + e.getMessage());
                });
    }

    /**
     * Release resources when done
     */
    public void close() {
        recognizer.close();
    }
}
