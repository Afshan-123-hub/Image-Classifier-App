package com.example.mlclassifier;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    Interpreter tflite;
    TextView txtResult;
    ImageView imageView;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResult = findViewById(R.id.txtResult);
        imageView = findViewById(R.id.imageView);
        Button btnSelect = findViewById(R.id.btnSelect);

        // 1. Model Load Karein (Assets folder se)
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            txtResult.setText("Model load failed: " + e.getMessage());
        }

        // 2. Button par click karne se gallery khulegi
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 10);
        });
    }

    // 3. Gallery se image wapis milne par ye chalega
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);

                // AI Model se check karwayein
                processImage(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Model file load karne ka function
    private MappedByteBuffer loadModelFile() throws Exception {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_unquant.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    // AI Processing Function
    public void processImage(Bitmap bitmap) {
        // Image preprocessing (224x224 size)
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        int[] intValues = new int[224 * 224];
        resized.getPixels(intValues, 0, resized.getWidth(), 0, 0, resized.getWidth(), resized.getHeight());

        for (int val : intValues) {
            inputBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
            inputBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
            inputBuffer.putFloat((val & 0xFF) / 255.0f);
        }

        // Output array
        float[][] output = new float[1][2];

        // Model Run
        if (tflite != null) {
            tflite.run(inputBuffer, output);

            // Result dikhayein
            if (output[0][0] > output[0][1]) {
                txtResult.setText("Result: Item A (" + (int)(output[0][0]*100) + "%)");
            } else {
                txtResult.setText("Result: Item B (" + (int)(output[0][1]*100) + "%)");
            }
        }
    }
}