package com.project.androidocr;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.UUID;

public class OcrScanner {
    //Permission Code
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 2001;

    String[] cameraPermission; // before String cameraPermission[];
    Uri image_uri;

    private final Context con;
    private final Activity act;
    private EditText mResultEt;
    private ImageView mPreviewIv;
    private Button mCTA;
    private ScrollView resultTxtScrollView;

    public OcrScanner(Context context) {
        con = context;
        act = (Activity) context;

        mResultEt   = act.findViewById(R.id.resultEt);
        mPreviewIv  = act.findViewById(R.id.imageIv);
        mCTA = act.findViewById(R.id.addImage);
        resultTxtScrollView = act.findViewById(R.id.scrollView2);

        mCTA.setOnClickListener(view -> {
            showCamera();
        });

        //camera permission
        cameraPermission = new String[] {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    public void watchOnRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean writeStorageAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;

                if (cameraAccepted && writeStorageAccepted) {
                    pickCamera();
                } else {
                    Toast.makeText(con, "Permissão Negada.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void watchOnActivityResult(int requestCode, int resultCode, Intent data) {
        int RESULT_OK = -1;

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //got image from camera now crop it
                CropImage.activity(image_uri)
                        .setAllowFlipping(false)
                        .setAutoZoomEnabled(true)
                        .setAspectRatio(1, 1)
                        .setInitialCropWindowPaddingRatio((float) 0.3)
                        .setMultiTouchEnabled(true)
                        .setFixAspectRatio(true)
                        .setBorderCornerOffset(90)
                        .setBorderCornerThickness(15)
                        .setBorderCornerLength(50)
                        .setAllowRotation(false)
                        .setBorderLineColor(Color.GREEN)
                        .setAllowRotation(false)
                        .setActivityTitle("Corte apenas a foto do chip")
                        .setCropMenuCropButtonIcon(R.drawable.ic_baseline_content_cut_24)
                        .setAllowCounterRotation(false)
                        .setGuidelinesColor(Color.TRANSPARENT)
                        .setGuidelines(CropImageView.Guidelines.ON) //enable image guid lines
                        .start(act);
            }
        }

        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            resultTxtScrollView.setVisibility(View.VISIBLE);
            mCTA.setText("Escanear outro chip");

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.d("TAG_resultUri", String.valueOf(resultUri));

                Bitmap urlBitmap = null;
                try {
                    urlBitmap = MediaStore.Images.Media.getBitmap(con.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // uploadBitmap(urlBitmap);

                //set image to image view
                mPreviewIv.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(con).build();

                if (!recognizer.isOperational()) {
                    Toast.makeText(con, "Ocorreu um erro. Tente novamente.", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    //get text from sb until there is no text
                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myItem = items.valueAt(i);
                        sb.append(myItem.getValue());
                        sb.append("\n");
                    }

                    //set text to edit text
                    final String onlyNumberResult = sb.toString().replaceAll("[^0-9]", "");
                    mResultEt.setText(onlyNumberResult);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //if there is any error show it
                Exception error = result.getError();
                Toast.makeText(con, "" + error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showCamera() {
        if (!checkCameraPermission()) {
            //camera permission not allowed, request it
            requestCameraPermission();
        } else {
            //permission allowed, take picture
            pickCamera();
        }
    }

    private void pickCamera() {
        //intent to take image from camera, it will also be save to storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPick" + getRandomString()); //title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To Text" + getRandomString()); //title of the picture
        image_uri = con.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        act.startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(act, cameraPermission, CAMERA_REQUEST_CODE);
    }

    // HELPERS
    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(con,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(con,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    public static String getRandomString() {
        return UUID.randomUUID().toString();
    }
}
