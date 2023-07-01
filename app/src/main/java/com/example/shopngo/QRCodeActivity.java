package com.example.shopngo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        // Get the ImageView from the layout
        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);

        // Generate QR code
        String qrData = "Hello, World!"; // Replace with your QR code data
        int qrCodeSize = 500; // Size of the QR code image in pixels

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }

            Bitmap qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            qrCodeBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // Set the QR code image to the ImageView
            qrCodeImageView.setImageBitmap(qrCodeBitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }

        // Initiate QR code scanning when ImageView is clicked
        qrCodeImageView.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan QR code"); // Set prompt message
            integrator.setOrientationLocked(false); // Set orientation to portrait
            integrator.initiateScan();
        });
    }

    // Override onActivityResult to handle the result of QR code scanning

    public void onScanButtonClick(View view) {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan QR code");

        integrator.initiateScan();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scannedData = result.getContents(); // Get the scanned QR code data
            Toast.makeText(this, "Scanned QR code: " + scannedData, Toast.LENGTH_SHORT).show();

            // Replace with your logic to handle the scanned QR code data, e.g., start a new intent
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("scannedData", scannedData); // Pass the scanned data to the new intent
            startActivity(intent);
        }
    }
}