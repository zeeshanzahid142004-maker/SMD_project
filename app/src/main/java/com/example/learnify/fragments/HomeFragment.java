package com.example.learnify.fragments;

import android. Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util. Log;
import android.view. LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx. annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx. recyclerview.widget.SnapHelper;

import com.example.learnify.helpers.CustomToast;
import com.example.learnify.helpers.DialogHelper;
import com.example.learnify.activities.HistoryActivity;
import com.example.learnify.adapters.HistoryAdapter;
import com.example.learnify.services.ImageTextExtractor;
import com.example.learnify.modelclass.QuizAttempt;
import com.example.learnify.repository.QuizAttemptRepository;
import com.example.learnify.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf. extractor.XWPFWordExtractor;
import org.apache. poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io. InputStream;
import java.io. InputStreamReader;
import java. text.SimpleDateFormat;
import java.util. ArrayList;
import java.util. Date;
import java.util.List;
import java.util. Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private OnHomeFragmentInteractionListener mListener;
    private CardView uploadFileButton;
    private CardView uploadLinkButton;
    private CardView uploadImageButton;
    private CardView profileCard;
    private TextView viewMoreButton;
    private TextView profileNameView;
    private ActivityResultLauncher<String[]> filePickerLauncher;

    // Camera and Gallery launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri cameraPhotoUri;

    // History views
    private RecyclerView rvRecentHistory;
    private CardView emptyHistoryCard;
    private HistoryAdapter historyAdapter;
    private List<QuizAttempt> recentAttempts = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // OCR Text Extractor
    private ImageTextExtractor imageTextExtractor;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeFragmentInteractionListener) {
            mListener = (OnHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context. toString() + " must implement OnHomeFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        try {
            PDFBoxResourceLoader.init(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "❌ PDFBox Init Failed", e);
        }

        // File picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        extractAndProcessFile(uri);
                    } else {
                        CustomToast.info(getContext(), getString(R.string.msg_no_file_selected));
                    }
                }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraPhotoUri != null) {
                            extractTextFromImage(cameraPhotoUri);
                        }
                    } else {
                        CustomToast.info(getContext(), getString(R.string.msg_camera_cancelled));
                    }
                }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result. getResultCode() == Activity. RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            extractTextFromImage(selectedImageUri);
                        }
                    } else {
                        CustomToast.info(getContext(), getString(R.string.msg_no_image_selected));
                    }
                }
        );

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts. RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        CustomToast.warning(getContext(), getString(R.string.msg_camera_permission_required));
                    }
                }
        );

        attemptRepository = new QuizAttemptRepository();

        // Initialize OCR text extractor
        imageTextExtractor = new ImageTextExtractor(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        uploadFileButton = view.findViewById(R.id.upload_file_button);
        uploadLinkButton = view.findViewById(R.id.upload_link_button);
        uploadImageButton = view.findViewById(R.id. upload_image_button);
        profileCard = view.findViewById(R.id.profile_card);
        viewMoreButton = view.findViewById(R.id.view_more_button);
        profileNameView = view.findViewById(R.id.profile_name);
        rvRecentHistory = view.findViewById(R.id. history_recycler_view);
        emptyHistoryCard = view. findViewById(R.id.empty_history_view);

        if (rvRecentHistory == null || emptyHistoryCard == null) return;

        loadUserName();

        profileCard.setOnClickListener(v -> {
            if (mListener != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        uploadFileButton. setOnClickListener(v -> {
            String[] mimeTypes = {
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
            };
            filePickerLauncher. launch(mimeTypes);
        });

        uploadLinkButton. setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onUploadLinkClicked();
            }
        });

        // Image upload button - Show Camera/Gallery Dialog
        if (uploadImageButton != null) {
            uploadImageButton.setOnClickListener(v -> showImageSourceDialog());
        }

        viewMoreButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), HistoryActivity.class));
        });

        setupRecentHistory();
    }

    /**
     * Show dialog to choose between Camera and Gallery
     */
    private void showImageSourceDialog() {
        DialogHelper.showImageSourceDialog(requireContext(), new DialogHelper.ImageSourceCallback() {
            @Override
            public void onCameraSelected() {
                openCamera();
            }

            @Override
            public void onGallerySelected() {
                openGallery();
            }
        });
    }

    /**
     * Open camera to take photo
     */
    private void openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager. PERMISSION_GRANTED) {
            cameraPermissionLauncher. launch(Manifest.permission. CAMERA);
            return;
        }
        launchCamera();
    }

    /**
     * Actually launch the camera
     */
    private void launchCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Check if there's a camera app available
            if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) == null) {
                CustomToast.error(getContext(), getString(R.string.error_no_camera_app));
                return;
            }

            // Create file to save the photo
            File photoFile = createImageFile();
            if (photoFile != null) {
                try {
                    cameraPhotoUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            photoFile
                    );
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    cameraLauncher.launch(takePictureIntent);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "FileProvider error", e);
                    CustomToast.error(getContext(), getString(R.string.error_camera_file_provider));
                }
            } else {
                CustomToast.error(getContext(), getString(R.string.error_create_image_file));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching camera", e);
            CustomToast.error(getContext(), getString(R.string.error_camera_launch));
        }
    }

    /**
     * Create a temporary file for camera photo
     */
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    /**
     * Open gallery to select image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void loadUserName() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if (displayName != null && !displayName.isEmpty()) {
                String firstName = displayName.split(" ")[0];
                profileNameView.setText(getString(R.string.welcome_user, firstName));
            } else if (email != null) {
                String emailPrefix = email.split("@")[0];
                profileNameView.setText(getString(R.string.welcome_user, emailPrefix));
            } else {
                profileNameView. setText(getString(R.string. welcome));
            }
        } else {
            profileNameView.setText(getString(R.string.welcome));
        }
    }

    /**
     * Setup recent history display - HORIZONTAL CAROUSEL EFFECT
     */
    private void setupRecentHistory() {
        Log.d(TAG, "📜 Setting up recent history view - Carousel Mode");

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvRecentHistory.setLayoutManager(layoutManager);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvRecentHistory);

        rvRecentHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super. onScrolled(recyclerView, dx, dy);
                updateCarouselScale(recyclerView);
            }
        });

        historyAdapter = new HistoryAdapter(recentAttempts, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                attempt.isFavorite = !attempt.isFavorite;
                attemptRepository.markAsFavorite(attempt. attemptId, attempt.isFavorite);
                historyAdapter.updateItem(position, attempt);
                if (attempt.isFavorite) {
                    CustomToast.success(getContext(), "Favourited!");
                } else {
                    CustomToast.info(getContext(), "Removed from favourites");
                }
            }
        });

        rvRecentHistory.setAdapter(historyAdapter);
        loadRecentHistory();
    }

    private void updateCarouselScale(RecyclerView recyclerView) {
        float minScale = 0.85f;
        float minAlpha = 0.5f;

        int centerX = recyclerView.getWidth() / 2;

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            int childCenterX = (child.getLeft() + child.getRight()) / 2;
            int distance = Math.abs(centerX - childCenterX);

            float scale = 1.0f - ((float) distance / centerX) * 0.4f;
            scale = Math.max(minScale, scale);

            child.setScaleX(scale);
            child.setScaleY(scale);

            float alpha = 1.0f - ((float) distance / centerX) * 0.5f;
            child.setAlpha(Math.max(minAlpha, alpha));
        }
    }

    private void loadRecentHistory() {
        attemptRepository.getRecentAttempts(5, new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> attempts) {
                recentAttempts.clear();
                recentAttempts.addAll(attempts);

                if (recentAttempts.isEmpty()) {
                    rvRecentHistory.setVisibility(View.GONE);
                    emptyHistoryCard.setVisibility(View.VISIBLE);
                } else {
                    rvRecentHistory.setVisibility(View.VISIBLE);
                    emptyHistoryCard. setVisibility(View.GONE);
                    historyAdapter.notifyDataSetChanged();
                    rvRecentHistory.post(() -> updateCarouselScale(rvRecentHistory));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load recent history", e);
                emptyHistoryCard.setVisibility(View.VISIBLE);
                rvRecentHistory.setVisibility(View.GONE);
            }
        });
    }

    private void extractAndProcessFile(Uri uri) {
        Log.d(TAG, "🔍 extractAndProcessFile() called with uri: " + uri);
        
        // Store context reference BEFORE starting thread
        Context context = getContext();
        Activity activity = getActivity();
        
        if (context == null || activity == null) {
            Log.e(TAG, "❌ Context or Activity is null, cannot process file");
            return;
        }
        
        Log.d(TAG, "✅ Context and Activity are valid");
        CustomToast.info(context, getString(R.string.msg_reading_file));

        new Thread(() -> {
            try {
                Log.d(TAG, "📄 Starting file extraction in background thread");
                String text = null;
                String type = context.getContentResolver().getType(uri);
                Log.d(TAG, "📄 File MIME type: " + type);

                try {
                    if (type != null && type.equals("application/pdf")) {
                        Log.d(TAG, "📄 Extracting PDF file");
                        text = extractPdf(uri, context);
                    } else if (type != null && (type.contains("wordprocessingml") || type.contains("msword"))) {
                        Log.d(TAG, "📄 Extracting DOCX file");
                        text = extractDocx(uri, context);
                    } else if (type != null && type.startsWith("text/")) {
                        Log.d(TAG, "📄 Extracting TXT file");
                        text = extractTxt(uri, context);
                    } else {
                        Log.d(TAG, "📄 Unknown type, trying PDF first");
                        try {
                            text = extractPdf(uri, context);
                        } catch (Exception e) {
                            Log.d(TAG, "📄 PDF failed, trying TXT");
                            text = extractTxt(uri, context);
                        }
                    }
                } catch (Exception extractionError) {
                    Log.e(TAG, "❌ File extraction method failed", extractionError);
                    throw extractionError;
                }

                final String extractedText = text;
                Log.d(TAG, "📝 Extraction complete. Text length: " + (extractedText != null ? extractedText.length() : "null"));

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Context uiContext = getContext();
                        if (uiContext == null) {
                            Log.e(TAG, "❌ UI Context is null after extraction");
                            return;
                        }
                        
                        if (extractedText != null && !extractedText.isEmpty()) {
                            Log.d(TAG, "✅ Launching quiz fragment with extracted text");
                            launchGenerateQuizFragment(extractedText);
                        } else {
                            Log.e(TAG, "❌ Extracted text is empty");
                            CustomToast.warning(uiContext, getString(R.string.msg_could_not_read_file));
                        }
                    });
                } else {
                    Log.e(TAG, "❌ Activity is null after extraction");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ FATAL File Extraction Error", e);
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Context uiContext = getContext();
                        if (uiContext != null) {
                            CustomToast.error(uiContext, getString(R.string.msg_error_reading_file, e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }

    private void launchGenerateQuizFragment(String text) {
        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(text);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                . addToBackStack(null)
                .commit();
    }

    private String extractPdf(Uri uri, Context context) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        PDDocument doc = PDDocument.load(is);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);
        doc.close();
        return text;
    }

    private String extractDocx(Uri uri, Context context) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        XWPFDocument doc = new XWPFDocument(is);
        XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
        String text = extractor.getText();
        extractor.close();
        return text;
    }

    private String extractTxt(Uri uri, Context context) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line). append("\n");
        }
        return sb.toString();
    }

    /**
     * Extract text from image using OCR (ML Kit Text Recognition)
     */
    private void extractTextFromImage(Uri imageUri) {
        Log.d(TAG, "📷 extractTextFromImage() called with uri: " + imageUri);
        
        Context context = getContext();
        Activity activity = getActivity();
        
        if (context == null || activity == null) {
            Log.e(TAG, "❌ Context or Activity is null for image extraction");
            return;
        }
        
        if (imageUri == null) {
            Log.e(TAG, "❌ Image URI is null");
            CustomToast.error(context, "No image selected");
            return;
        }
        
        Log.d(TAG, "✅ Starting OCR extraction");
        CustomToast.info(context, getString(R.string.msg_scanning_image));

        if (imageTextExtractor == null) {
            Log.d(TAG, "🔧 Initializing ImageTextExtractor");
            try {
                imageTextExtractor = new ImageTextExtractor(requireContext());
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to initialize ImageTextExtractor", e);
                CustomToast.error(context, "OCR initialization failed");
                return;
            }
        }
        
        // Add try-catch around the extraction
        try {
            imageTextExtractor.extractTextFromUri(imageUri, new ImageTextExtractor.ExtractionCallback() {
                @Override
                public void onSuccess(String extractedText) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.d(TAG, "✅ OCR Success: " + extractedText.length() + " characters extracted");
                            
                            Context uiContext = getContext();
                            if (uiContext == null) {
                                Log.e(TAG, "❌ UI Context is null after OCR");
                                return;
                            }

                            if (extractedText.trim().isEmpty()) {
                                Log.d(TAG, "⚠️ Extracted text is empty");
                                CustomToast.warning(uiContext, getString(R.string.msg_no_text_in_image));
                                return;
                            }

                            CustomToast.success(uiContext, getString(R.string.msg_text_extracted));
                            launchGenerateQuizFragment(extractedText);
                        });
                    } else {
                        Log.e(TAG, "❌ Activity is null after OCR success");
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Log.e(TAG, "❌ OCR Error: " + error);
                            Context uiContext = getContext();
                            if (uiContext != null) {
                                CustomToast.error(uiContext, getString(R.string.msg_ocr_failed, error));
                            }
                        });
                    } else {
                        Log.e(TAG, "❌ Activity is null after OCR error");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in extractTextFromUri", e);
            e.printStackTrace();
            CustomToast.error(context, "OCR failed: " + e.getMessage());
        }
    }

    public interface OnHomeFragmentInteractionListener {
        void onViewMoreHistoryClicked();
        void onGoToVideoFragment(String url);
        void onGoToQuizFragment(String url);
        void onUploadLinkClicked();
        void onGoToVideoFragment(String videoUrl, String transcript);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentHistory();
        loadUserName();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageTextExtractor != null) {
            imageTextExtractor.close();
            imageTextExtractor = null;
        }
    }
}