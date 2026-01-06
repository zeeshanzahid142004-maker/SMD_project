package com.example.learnify.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
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
        Log.d(TAG, "🔍 extractTextFromUri() called with uri: " + imageUri);
        
        if (imageUri == null) {
            Log.e(TAG, "❌ Image URI is null");
            callback.onError("Image URI is null");
            return;
        }
        
        try {
            Log.d(TAG, "📷 Creating InputImage from URI");
            InputImage image = InputImage.fromFilePath(context, imageUri);
            
            if (image == null) {
                Log.e(TAG, "❌ InputImage is null after creation");
                callback.onError("Failed to create image from URI");
                return;
            }
            
            Log.d(TAG, "✅ InputImage created successfully, processing...");
            processImage(image, callback);
        } catch (IOException e) {
            Log.e(TAG, "❌ IOException while creating InputImage from URI", e);
            e.printStackTrace();
            callback.onError("Failed to load image: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected exception in extractTextFromUri", e);
            e.printStackTrace();
            callback.onError("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Extract text from a Bitmap
     * @param bitmap Bitmap image to process
     * @param callback Callback to receive extracted text or error
     */
    public void extractTextFromBitmap(Bitmap bitmap, ExtractionCallback callback) {
        Log.d(TAG, "🔍 extractTextFromBitmap() called");
        
        if (bitmap == null) {
            Log.e(TAG, "❌ Bitmap is null");
            callback.onError("Bitmap is null");
            return;
        }
        
        try {
            Log.d(TAG, "📷 Creating InputImage from Bitmap");
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            if (image == null) {
                Log.e(TAG, "❌ InputImage is null after creation from bitmap");
                callback.onError("Failed to create image from bitmap");
                return;
            }
            
            Log.d(TAG, "✅ InputImage created from bitmap, processing...");
            processImage(image, callback);
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in extractTextFromBitmap", e);
            e.printStackTrace();
            callback.onError("Failed to process bitmap: " + e.getMessage());
        }
    }

    /**
     * Process the InputImage with ML Kit Text Recognition
     */
    private void processImage(InputImage image, ExtractionCallback callback) {
        Log.d(TAG, "🔄 Processing image with ML Kit Text Recognition");
        
        if (image == null) {
            Log.e(TAG, "❌ InputImage is null in processImage");
            callback.onError("Input image is null");
            return;
        }
        
        if (recognizer == null) {
            Log.e(TAG, "❌ Text recognizer is null");
            callback.onError("Text recognizer not initialized");
            return;
        }
        
        try {
            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        Log.d(TAG, "🎉 Text recognition successful");
                        if (text == null) {
                            Log.e(TAG, "❌ Recognition succeeded but text object is null");
                            callback.onError("No text object returned");
                            return;
                        }
                        
                        String extractedText = text.getText();
                        if (extractedText == null || extractedText.trim().isEmpty()) {
                            Log.d(TAG, "⚠️ No text found in image");
                            callback.onError("No text found in image");
                        } else {
                            Log.d(TAG, "✅ Extracted " + extractedText.length() + " characters from image");
                            callback.onSuccess(extractedText);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Text recognition failed", e);
                        e.printStackTrace();
                        callback.onError("Text recognition failed: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception while processing image", e);
            e.printStackTrace();
            callback.onError("Exception during recognition: " + e.getMessage());
        }
    }

    /**
     * Release resources when done
     */
    public void close() {
        recognizer.close();
    }
}
