package cordova.plugin.zoomvideo;

import static cordova.plugin.zoomvideo.SessionActivity.getResourceId;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.IOException;

// The class name now matches the new file name
public class AttachmentViewerDialogFragment extends DialogFragment {

    private WebView webView;
    private ImageView pdfImageView;
    private RelativeLayout pdfNavigationControls;
    private Button pdfButtonPrevious;
    private Button pdfButtonNext;
    private Button buttonClose;
    private TextView pdfPageNumber;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;
    private int currentPageIndex = 0;

    // --- ADD A MEMBER VARIABLE TO HOLD THE FILE PATH ---
    private String mFilePath;
    private String mFileType;

    final String LAYOUT = "layout";
    final String ID = "id";

    public static AttachmentViewerDialogFragment newInstance(String mimeType, String filePath) {
        AttachmentViewerDialogFragment fragment = new AttachmentViewerDialogFragment();
        Bundle args = new Bundle();
        args.putString("mimeType", mimeType);
        args.putString("filePath", filePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // --- RETRIEVE ARGUMENTS HERE AND STORE THEM ---
        if (getArguments() != null) {
            mFilePath = getArguments().getString("filePath");
            mFileType = getArguments().getString("mimeType");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getResourceId(getContext(), LAYOUT, "activity_attachment_viewer"), container, false);
        Context context = getContext();

        // All of your findViewById and setup logic
        webView = view.findViewById(getResourceId(context,ID,("webview_viewer")));
        pdfImageView = view.findViewById(getResourceId(context, ID, ("pdf_image_view_viewer")));
        pdfNavigationControls = view.findViewById(getResourceId(context, ID, "pdf_navigation_controls_viewer"));
        pdfButtonPrevious = view.findViewById(getResourceId(context, ID, "pdf_button_previous_viewer"));
        pdfButtonNext = view.findViewById(getResourceId(context, ID, "pdf_button_next_viewer"));
        pdfPageNumber = view.findViewById(getResourceId(context, ID, "pdf_page_number_viewer"));
        buttonClose = view.findViewById(getResourceId(context, ID, "close_button_viewer"));

        buttonClose.setOnClickListener(v -> {
            // Dismisses the DialogFragment
            closePdfRenderer();
            deleteTempFile();
            dismiss();
        });
        pdfButtonPrevious.setOnClickListener(v -> showPdfPage(currentPageIndex - 1));
        pdfButtonNext.setOnClickListener(v -> showPdfPage(currentPageIndex + 1));

        // Check for nulls before proceeding
        if (mFileType == null || mFilePath == null) {
            Log.e("AttachmentViewer", "MIME type or file path is missing.");
            dismiss();
            return view; // Return early
        }

        // Logic for handling files
        File file = new File(mFilePath);
        if (!file.exists()) {
            Log.e("AttachmentViewer", "File does not exist at path: " + mFilePath);
            dismiss();
        } else if (mFileType.startsWith("image/")) {
            webView.setVisibility(View.VISIBLE);
            pdfImageView.setVisibility(View.GONE);
            pdfNavigationControls.setVisibility(View.GONE);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setAllowFileAccess(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false);
            webView.getSettings().setLoadWithOverviewMode(true);
            webView.getSettings().setUseWideViewPort(true);
            String html = "<html><body style='margin:0;padding:0;background-color:black;display:flex;justify-content:center;align-items:center;'><img src='" + file.toURI() + "' style='width:100%;max-width:100%;height:auto;'/></body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        } else if (mFileType.equals("application/pdf")) {
            webView.setVisibility(View.GONE);
            pdfImageView.setVisibility(View.VISIBLE);
            pdfNavigationControls.setVisibility(View.VISIBLE);
            openPdfWithRenderer(file);
        } else {
            webView.setVisibility(View.VISIBLE);
            webView.loadData("<html><body>Preview not available for this file type.</body></html>", "text/html", "UTF-8");
        }

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            // Get the window of the dialog
            if (dialog.getWindow() != null) {
                // Get the screen height
                int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.80);

                // Set the dialog's dimensions
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);

                int animationStyleId = getResources().getIdentifier("DialogAnimation", "style", getContext().getPackageName());
                if (animationStyleId != 0) { // Check if the style was found
                    // Set the animation
                    dialog.getWindow().setWindowAnimations(animationStyleId);
                }
            }
        }
    }

    // In AttachmentViewerDialogFragment.java

    @Override
    public void onResume() {
        super.onResume();    // Apply the custom animation style
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Find the style's resource ID
            int animationStyleId = getResources().getIdentifier("DialogAnimation", "style", getContext().getPackageName());

            // Set the animation
            getDialog().getWindow().setWindowAnimations(animationStyleId);
        }
    }


//    @Override
//    public void onDismiss(@NonNull Dialog dialog) {
//        super.onDismiss(dialog);
//        // Cleanup resources when the dialog is dismissed
//        closePdfRenderer();
//        deleteTempFile();
//    }

    private void deleteTempFile() {
        if (mFilePath != null) {
            File tempFile = new File(mFilePath);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void closePdfRenderer() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            }
        } catch (IOException e) {
            Log.e("AttachmentViewer", "Error closing PDF resources", e);
        }
    }

    private void openPdfWithRenderer(File file) {
        try {
            closePdfRenderer(); // Close any existing renderer
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            currentPageIndex = 0;
            showPdfPage(currentPageIndex);
        } catch (IOException e) {
            Log.e("PDFRenderer", "Error opening PDF from file", e);
        }
    }

    private void showPdfPage(int index) {
        if (pdfRenderer == null || index < 0 || index >= pdfRenderer.getPageCount()) return;
        if (currentPage != null) currentPage.close();

        currentPage = pdfRenderer.openPage(index);
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        pdfImageView.setImageBitmap(bitmap);

        currentPageIndex = index;
        updatePdfNavigationButtons();
    }

    private void updatePdfNavigationButtons() {
        if (pdfRenderer == null) return;
        int pageCount = pdfRenderer.getPageCount();
        pdfButtonPrevious.setEnabled(currentPageIndex > 0);
        pdfButtonNext.setEnabled(currentPageIndex < pageCount - 1);
        pdfPageNumber.setText("Page " + (currentPageIndex + 1) + " / " + pageCount);
    }
}
