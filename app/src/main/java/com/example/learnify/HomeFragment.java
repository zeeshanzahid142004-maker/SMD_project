package com.example.learnify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget. Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx. annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox. pdmodel.PDDocument;
import com. tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf. usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io. InputStreamReader;
import java. util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private OnHomeFragmentInteractionListener mListener;
    private CardView uploadFileButton;
    private CardView uploadLinkButton;
    private TextView viewMoreButton;
    private ActivityResultLauncher<String[]> filePickerLauncher;

    // History views
    private RecyclerView rvRecentHistory;
    private LinearLayout llEmptyHistory;
    private HistoryAdapter historyAdapter;
    private List<QuizAttempt> recentAttempts = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;

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
                        Log.d(TAG, "📂 File selected: " + uri. toString());
                        extractAndProcessFile(uri);
                    } else {
                        Log.w(TAG, "⚠️ File picker returned null URI");
                        Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        attemptRepository = new QuizAttemptRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log. d(TAG, "🖼️ HomeFragment View Created");

        // Initialize views
        uploadFileButton = view. findViewById(R.id.upload_file_button);
        uploadLinkButton = view.findViewById(R.id.upload_link_button);
        viewMoreButton = view.findViewById(R.id.view_more_button);
        rvRecentHistory = view.findViewById(R.id.history_recycler_view);
        llEmptyHistory = view.findViewById(R.id.empty_history_view);

        // Setup upload buttons
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

        // ⭐ VIEW MORE BUTTON - Open HistoryActivity
        viewMoreButton.setOnClickListener(v -> {
            Log.d(TAG, "📜 View More History clicked");
            startActivity(new Intent(getActivity(), HistoryActivity.class));
        });

        // Setup recent history RecyclerView
        setupRecentHistory();
    }

    /**
     * Setup recent history display
     */
    private void setupRecentHistory() {
        Log.d(TAG, "📜 Setting up recent history view");

        rvRecentHistory.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        historyAdapter = new HistoryAdapter(recentAttempts, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                // Redirect to HistoryActivity for download
                Log.d(TAG, "Redirecting to History for download");
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                // Toggle favourite locally
                attempt.isFavorite = !attempt.isFavorite;
                attemptRepository.markAsFavorite(attempt. attemptId, attempt.isFavorite);
                historyAdapter.updateItem(position, attempt);
                String msg = attempt.isFavorite ?  "❤️ Favourited!" : "Removed";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        rvRecentHistory.setAdapter(historyAdapter);

        // Load recent attempts
        loadRecentHistory();
    }

    /**
     * Load recent quiz attempts (up to 5)
     */
    private void loadRecentHistory() {
        Log.d(TAG, "📥 Loading recent history (up to 5)");

        attemptRepository.getRecentAttempts(5, new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> attempts) {
                recentAttempts.clear();
                recentAttempts.addAll(attempts);

                if (recentAttempts.isEmpty()) {
                    // Show empty state
                    rvRecentHistory.setVisibility(View.GONE);
                    llEmptyHistory.setVisibility(View.VISIBLE);
                    Log.d(TAG, "📭 No recent history - showing empty state");
                } else {
                    // Show history
                    rvRecentHistory.setVisibility(View. VISIBLE);
                    llEmptyHistory.setVisibility(View.GONE);
                    historyAdapter.notifyDataSetChanged();
                    Log.d(TAG, "✅ Loaded " + recentAttempts.size() + " recent attempts");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load recent history", e);
                llEmptyHistory.setVisibility(View.VISIBLE);
                rvRecentHistory.setVisibility(View.GONE);
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
                    Log.d(TAG, "⚙️ Extracting PDF.. .");
                    text = extractPdf(uri);
                } else if (type != null && (type.contains("wordprocessingml") || type.contains("msword"))) {
                    Log.d(TAG, "⚙️ Extracting DOCX...");
                    text = extractDocx(uri);
                } else if (type != null && type.startsWith("text/")) {
                    Log.d(TAG, "⚙️ Extracting TXT...");
                    text = extractTxt(uri);
                } else {
                    Log.w(TAG, "⚠️ Unknown MIME type.  Attempting PDF fallback.");
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
                            Log. e(TAG, "❌ Extraction result was empty or null");
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
        Log. d(TAG, "🚀 Launching GenerateQuizFragment.. .");
        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(text);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

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
        String text = extractor. getText();
        extractor.close();
        Log. d(TAG, "📖 DOCX extracted.");
        return text;
    }

    private String extractTxt(Uri uri) throws Exception {
        InputStream is = getContext().getContentResolver().openInputStream(uri);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line). append("\n");
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
    public void onResume() {
        super.onResume();
        // Refresh history when returning
        loadRecentHistory();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}