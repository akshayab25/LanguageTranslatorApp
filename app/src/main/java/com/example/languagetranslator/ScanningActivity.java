package com.example.languagetranslator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptions;

import static android.Manifest.permission.CAMERA;

public class ScanningActivity extends AppCompatActivity {
    private ImageView captureIV;
    private TextView resultTV,translatedTV;
    private Button snapBtn, detectBtn;
    public Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE=1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);
        Intent intent=getIntent();
        int fromLanguageCode=intent.getIntExtra("FromLanguage",0);
        int toLanguageCode=intent.getIntExtra("ToLanguage",0);
        captureIV = findViewById(R.id.idIVCaptureImage);
        resultTV = findViewById(R.id.idTVDetectedText);
        snapBtn = findViewById(R.id.idBtnSnap);
        detectBtn = findViewById(R.id.idBtnDetect);
        translatedTV= findViewById(R.id.idTVTranslatedText);

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectText();
                translatedTV.setText("");
                if(resultTV.getText().toString().isEmpty()){
                    Toast.makeText(ScanningActivity.this,"Please enter text to translate",Toast.LENGTH_SHORT).show();
                }
                else if(fromLanguageCode==0){
                    Toast.makeText(ScanningActivity.this,"Please select source language",Toast.LENGTH_SHORT).show();
                }
                else if(toLanguageCode==0){
                    Toast.makeText(ScanningActivity.this,"Please select language to make translation",Toast.LENGTH_SHORT).show();
                }
                else{
                    translateText(fromLanguageCode,toLanguageCode,resultTV.getText().toString());
                }
            }
        });
        snapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermissions()){
                    captureImage();
                }
                else{
                    requestPermission();
                }

            }
        });
    }
    private boolean checkPermissions(){
        int cameraPermission= ContextCompat.checkSelfPermission(getApplicationContext(),CAMERA);
        return cameraPermission== PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission(){
        int PERMISSION_CODE=200;
        ActivityCompat.requestPermissions(this,new String[]{CAMERA},PERMISSION_CODE);
    }

    protected void captureImage(){
        Intent takePicture = new Intent((MediaStore.ACTION_IMAGE_CAPTURE));
        if(takePicture.resolveActivity(getPackageManager())!=null){
            startActivityForResult(takePicture,REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            boolean cameraPermission=grantResults[0]==PackageManager.PERMISSION_GRANTED;
            if(cameraPermission){
                Toast.makeText(this,"Permission has been granted!",Toast.LENGTH_SHORT).show();
                captureImage();
            }else{Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK){
            Bundle extras=data.getExtras();
            imageBitmap=(Bitmap) extras.get("data");
            captureIV.setImageBitmap(imageBitmap);
        }
    }

    private void detectText(){
        InputImage image=InputImage.fromBitmap(imageBitmap,0);
        TextRecognizer recognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result=recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result=new StringBuilder();
                for(Text.TextBlock block:text.getTextBlocks()){
                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for(Text.Line line:block.getLines()){
                        String lineText=line.getText();
                        Point[] lineCornerPoints=line.getCornerPoints();
                        Rect lineRect=line.getBoundingBox();
                        for(Text.Element element:line.getElements()){
                            String elementText = element.getText();
                            result.append(elementText);
                        }
                        resultTV.setText(blockText);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScanningActivity.this,"Failed to detect text from Image.."+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void translateText(int fromLanguageCode, int toLanguageCode, String sourceText) {
        translatedTV.setText("Downloading Model...");
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(fromLanguageCode)
                .setTargetLanguage(toLanguageCode)
                .build();

        final FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance()
                .getTranslator(options);

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .build();

        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                translatedTV.setText("Translating...");
                translator.translate(sourceText).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        translatedTV.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ScanningActivity.this, "Failed to Translate : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScanningActivity.this, "Failed to download language modal " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}