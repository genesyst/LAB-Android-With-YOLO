package com.genesyst.lab.firstyolo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button predictBtn,SelectBtn,TakephotoBtn;
    private Bitmap bitmap;
    private ProgressBar progressBar;
    private LinearLayout controlBtnPanel;

    private int REQUEST_PICK_IMAGE = 1001;

    private Yolov5TFLiteDetector yolov5TFLiteDetector;
    Paint boxPaint = new Paint();
    Paint textPain  = new Paint();
    private int TAKEPHOTO_REQUEST = 1002;

    private ArrayList<String> ConfidenceSumm = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.SetView();

        this.yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        this.yolov5TFLiteDetector.setModelFile("yolov5s-fp16.tflite");
        this.yolov5TFLiteDetector.initialModel(this);

        this.boxPaint.setStrokeWidth(5);
        this.boxPaint.setStyle(Paint.Style.STROKE);
        this.boxPaint.setColor(Color.RED);

        this.textPain.setTextSize(50);
        this.textPain.setColor(Color.GREEN);
        this.textPain.setStyle(Paint.Style.FILL);
    }

    private void SetView(){
        this.controlBtnPanel = (LinearLayout)findViewById(R.id.controlBtnPanel);
        this.imageView = (ImageView) findViewById(R.id.imageView);
        this.predictBtn = (Button) findViewById(R.id.predictBtn);
        this.SelectBtn = (Button) findViewById(R.id.SelectBtn);
        this.TakephotoBtn = (Button) findViewById(R.id.TakephotoBtn);
        this.progressBar = (ProgressBar)findViewById(R.id.progress);
        this.progressBar.setVisibility(View.GONE);

        this.SetAction();
    }

    private void SetAction(){
        this.SelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,REQUEST_PICK_IMAGE);
            }
        });

        this.TakephotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, TAKEPHOTO_REQUEST);
                } else {
                    //Request camera permission if we don't have it.
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
                }
            }
        });

        this.predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bitmap!=null) {
                    ConfidenceSumm.clear();
                    controlBtnPanel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            Predict();
                        }
                    });
                }
            }
        });
    }

    private void Predict(){
        try {
            ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(bitmap);

            Bitmap mtableBM = this.bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mtableBM);

            for (Recognition recognition : recognitions) {
                if (recognition.getConfidence() > 0.4) {
                    RectF location = recognition.getLocation();
                    canvas.drawRect(location, boxPaint);

                    Float confidence = recognition.getConfidence();
                    int percentage = (int) Math.ceil(confidence * 100);
                    String TextCaption = recognition.getLabelName() + " (" + percentage + "%)";

                    canvas.drawText(TextCaption, location.left, location.top, textPain);

                    this.ConfidenceSumm.add(TextCaption);
                }
            }

            this.imageView.setImageBitmap(mtableBM);

            if(this.ConfidenceSumm.size() > 0) {
                Intent ConfidIntent = new Intent(this, ConfidListActivity.class);
                ConfidIntent.putExtra("data", this.ConfidenceSumm);
                startActivity(ConfidIntent);
            }
        }finally {
            this.progressBar.setVisibility(View.GONE);
            this.controlBtnPanel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_PICK_IMAGE && data!=null){
            Uri uri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                this.bitmap = this.handleOrientation(inputStream,uri);
                this.imageView.setImageBitmap(this.bitmap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else if(requestCode == TAKEPHOTO_REQUEST && resultCode == Activity.RESULT_OK && data!=null){
            this.bitmap = (Bitmap) data.getExtras().get("data");
            this.imageView.setImageBitmap(this.bitmap);
        }
    }

    private Bitmap handleOrientation(InputStream inputStream, Uri uri) {
        if (inputStream != null) {
            try {
                ExifInterface exif;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
                        if (pfd != null) {
                            exif = new ExifInterface(pfd.getFileDescriptor());
                        } else {
                            throw new IOException("Failed to open file descriptor for URI: " + uri);
                        }
                    }
                } else {
                    exif = new ExifInterface(uri.getPath());
                }

                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                );

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return rotateImage(bitmap, 90f);
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return rotateImage(bitmap, 180f);
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return rotateImage(bitmap, 270f);
                    default:
                        return bitmap;
                }
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception as needed
            }
        }

        throw new IllegalArgumentException("Invalid input stream or URI");
    }


    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}