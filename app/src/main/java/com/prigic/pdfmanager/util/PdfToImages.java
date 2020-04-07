package com.prigic.pdfmanager.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.prigic.pdfmanager.fragment.PdfToImageFragment;
import com.prigic.pdfmanager.interfaces.ExtractImagesListener;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static com.prigic.pdfmanager.util.FileUtils.getFileNameWithoutExtension;
import static com.prigic.pdfmanager.util.FileUtils.saveImage;

public class PdfToImages extends AsyncTask<Void, Void, Void> {

    private String mPath;
    private Uri mUri;
    private ExtractImagesListener mExtractImagesListener;
    private int mImagesCount = 0;
    private ArrayList<String> mOutputFilePaths;

    public PdfToImages(String mPath, Uri mUri, ExtractImagesListener mExtractImagesListener) {
        this.mPath = mPath;
        this.mUri = mUri;
        this.mExtractImagesListener = mExtractImagesListener;
        mOutputFilePaths = new ArrayList<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mExtractImagesListener.extractionStarted();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mOutputFilePaths = new ArrayList<>();
        mImagesCount = 0;

        ParcelFileDescriptor fileDescriptor = null;
        try {
            if (mUri != null)
                fileDescriptor = ((PdfToImageFragment) mExtractImagesListener).getContext()
                        .getContentResolver().openFileDescriptor(mUri, "r");
            else if (mPath != null)
                fileDescriptor = ParcelFileDescriptor.open(new File(mPath), MODE_READ_ONLY);
            if (fileDescriptor != null) {
                PdfRenderer renderer = new PdfRenderer(fileDescriptor);
                final int pageCount = renderer.getPageCount();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    page.close();

                    String filename = getFileNameWithoutExtension(mPath) +
                            "_" + Integer.toString(i + 1);
                    String path = saveImage(filename, bitmap);
                    if (path != null) {
                        mOutputFilePaths.add(path);
                        mImagesCount++;
                    }
                }

                renderer.close();
            }
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mExtractImagesListener.updateView(mImagesCount, mOutputFilePaths);
    }
}
