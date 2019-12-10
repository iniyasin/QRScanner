package com.kilasbalik.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ImageView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private CodeScanner codeScanner;
    private CodeScannerView codeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.ivBgContent);
        codeScannerView = findViewById(R.id.scannerView);

        imageView.bringToFront();

        codeScanner = new CodeScanner(this, codeScannerView);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = result.getText();
                        String toBinary = stringToBinary(message);

                        String freq = "{ =110, a=011110, d=0000, e=1000, f=011100, g=01011, h=011101, I=011001, i=1110, L=011000, l=011010, m=1001, n=1011, .=011111, o=01010, p=0001, r=11110, s=1010, t=001, u=11111, x=011011, y=0100}";
                        String decode = decodeHuffman(toBinary, freq);

                        String result = "result :\n" + decode;
                        showAlertDialog(result);
                    }
                });
            }
        });

        checkCameraPermission();
    }

    private void checkCameraPermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        codeScanner.startPreview();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void showAlertDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(true);

        builder.setPositiveButton(
                "Scan Lagi",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        codeScanner.startPreview();
                    }
                }
        );

        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }
        );

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCameraPermission();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }

    private static String toBinary(String message){
        byte[] bytes = message.getBytes();
        StringBuilder stringBuilder = new StringBuilder();

        for (byte b : bytes){
            int val = b;
            for (int i = 0; i < 8; i++){
                stringBuilder.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }

        System.out.println(stringBuilder.toString());
        return stringBuilder.toString();
    }

    private static String decodeHuffman(String message, String frequency){

        //Change freq string to hashmap
        frequency = frequency.substring(1, frequency.length()-1); //remove the curly braces
        String[] keyValuePairs = frequency.split(",");
        HashMap<String, String> freq = new HashMap<>();

        //for lop to change string sequence to hashmap
        for (String pair : keyValuePairs){
            String[] entry = pair.split("=");
            freq.put(entry[1], entry[0]);
        }

        //using for loop to change codeword to char array
        char[] charCodeword = message.toCharArray();

        StringBuilder result = new StringBuilder(); //initiate result
        StringBuilder temp = new StringBuilder();// initiate temporary string memory

        //search and match codeword with map
        for (char c : charCodeword){
            temp.append(c);

            String match = freq.get(temp.toString());
            if (match == null){
                continue;
            }
            result.append(match);
            temp.setLength(0);
        }
        return result.toString();
    }

    public static String stringToBinary(String binary) {
        String bString="";
        String temp="";
        for(int i=0;i<binary.length();i++) {
            temp = Integer.toBinaryString(binary.charAt(i));
            for (int j = temp.length(); j < 8; j++) {
                temp = "0" + temp;
            }
            bString += temp;
        }
        return bString;
    }

}
