package com.example.videoeditornougat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private final int GALLERY = 1;
    public static Uri videoUri;
    private TextView header_title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        header_title = findViewById(R.id.head_name);

        ImageButton chooseVideoImageButton = findViewById(R.id.choose_video_img_button);
        chooseVideoImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallery();
            }
        });

        Button chooseVideoButton = findViewById(R.id.choose_video_button);
        chooseVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallery();
            }
        });
    }

    private void chooseVideoFromGallery() {
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), GALLERY);
        } catch (Exception e) {
            e.printStackTrace();
            header_title.setText("Ошибка при попытке выбора видео");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY) {
            videoUri = data.getData();

            Intent intent = new Intent(getApplicationContext(), VideoEditorActivity.class);
            startActivity(intent);
        }

    }

}