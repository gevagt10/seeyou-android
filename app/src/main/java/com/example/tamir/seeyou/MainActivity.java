package com.example.tamir.seeyou;

import android.content.Intent;
import android.graphics.Camera;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.TreeMap;

public class MainActivity extends AppCompatActivity //implements PictureCapturingListener{
{
    private APictureCapturingService pictureService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //pictureService = PictureCapturingServiceImpl.getInstance(this);

        Intent intent = new Intent(getApplicationContext(), MainService.class);
        startService(intent);
        //finish();
    }

//    @Override
//    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
//        Log.e("ALL",pictureUrl);
//    }
//
//    @Override
//    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
//
//    }

}
