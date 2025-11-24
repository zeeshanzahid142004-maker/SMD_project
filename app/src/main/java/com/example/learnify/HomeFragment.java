package com.example.learnify;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragmentDebug"; // DEBUG TAG

    private OnHomeFragmentInteractionListener mListener;
    private CardView uploadFileButton;
    private CardView uploadLinkButton;
    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "✅ HomeFragment Attached");
        if (context instanceof OnHomeFragmentInteractionListener) {
            mListener = (OnHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnHomeFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🛠️ HomeFragment onCreate");

        try {
            PDFBoxResourceLoader.init(requireContext());
            Log.d(TAG, "✅ PDFBox Initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ PDFBox Init Failed", e);
        }

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "📂 File selected: " + uri.toString());
                        extractAndProcessFile(uri);
                    } else {
                        Log.w(TAG, "⚠️ File picker returned null URI");
                        Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "🖼️ HomeFragment View Created");

        uploadFileButton = view.findViewById(R.id.upload_file_button);
        uploadLinkButton = view.findViewById(R.id.upload_link_button);

        uploadFileButton.setOnClickListener(v -> {
            Log.d(TAG, "🖱️ File Upload Clicked");
            String[] mimeTypes = {
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
            };
            filePickerLauncher.launch(mimeTypes);
        });

        uploadLinkButton.setOnClickListener(v -> {
            Log.d(TAG, "🖱️ Link Upload Clicked");
            if (mListener != null) {
                mListener.onUploadLinkClicked();
            }
        });
    }

    private void extractAndProcessFile(Uri uri) {
        Log.d(TAG, "⏳ Starting background extraction for: " + uri);
        Toast.makeText(getContext(), "Reading file...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String text = null;
            String type = getContext().getContentResolver().getType(uri);
            Log.d(TAG, "📄 File MIME type: " + type);

            try {
                if (type != null && type.equals("application/pdf")) {
                    Log.d(TAG, "⚙️ Extracting PDF...");
                    text = extractPdf(uri);
                } else if (type != null && (type.contains("wordprocessingml") || type.contains("msword"))) {
                    Log.d(TAG, "⚙️ Extracting DOCX...");
                    text = extractDocx(uri);
                } else if (type != null && type.startsWith("text/")) {
                    Log.d(TAG, "⚙️ Extracting TXT...");
                    text = extractTxt(uri);
                } else {
                    Log.w(TAG, "⚠️ Unknown MIME type. Attempting PDF fallback.");
                    try {
                        text = extractPdf(uri);
                    } catch (Exception e) {
                        Log.w(TAG, "⚠️ PDF fallback failed. Attempting TXT fallback.");
                        text = extractTxt(uri);
                    }
                }

                final String extractedText = text;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (extractedText != null && !extractedText.isEmpty()) {
                            Log.d(TAG, "✅ Extraction SUCCESS! Length: " + extractedText.length() + " chars");
                            launchGenerateQuizFragment(extractedText);
                        } else {
                            Log.e(TAG, "❌ Extraction result was empty or null");
                            Toast.makeText(getContext(), "Could not read text from this file.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ FATAL File Extraction Error", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private void launchGenerateQuizFragment(String text) {
        Log.d(TAG, "🚀 Launching GenerateQuizFragment...");
        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(text);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // --- Helpers with Logs ---

    private String extractPdf(Uri uri) throws Exception {
        InputStream is = getContext().getContentResolver().openInputStream(uri);
        PDDocument doc = PDDocument.load(is);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);
        doc.close();
        Log.d(TAG, "📖 PDF extracted. Pages: " + doc.getNumberOfPages());
        return text;
    }

    private String extractDocx(Uri uri) throws Exception {
        InputStream is = getContext().getContentResolver().openInputStream(uri);
        XWPFDocument doc = new XWPFDocument(is);
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
        String text = extractor.getText();
        extractor.close();
        Log.d(TAG, "📖 DOCX extracted.");
        return text;
    }

    private String extractTxt(Uri uri) throws Exception {
        InputStream is = getContext().getContentResolver().openInputStream(uri);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append("\n");
        }
        Log.d(TAG, "📖 TXT extracted.");
        return sb.toString();
    }

    public interface OnHomeFragmentInteractionListener {
        void onViewMoreHistoryClicked();
        void onGoToVideoFragment(String url);
        void onGoToQuizFragment(String url);
        void onUploadLinkClicked();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}