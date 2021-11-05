package com.example.videoeditornougat;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;


import org.florescu.android.rangeseekbar.RangeSeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;


public class VideoEditorActivity extends AppCompatActivity {

    private VideoView videoPlayer;
    private RelativeLayout videoLayout;
    public static Uri videoUri;
    private int stopPosition;

    private ImageButton playButton;
    private ImageButton goBackButton;
    private Button saveVideo;


    private final String INTERNAL_STORAGE_FILENAME = "edited_file.mp4";

    private RangeSeekBar rangeSeekBar;

    private ImageButton cropVideoButton;

    private FFmpeg ffrmpeg;

    private Runnable runnable;

    private String currentTime, endTime;
    private TextView timeBar;

    private int duration;
    // states
    private final int PLAY_STATE=100;
    private final int PAUSE_STATE=101;
    private final int CROP_STATE_PLAY=200;
    private final int CROP_STATE_PAUSE=201;

    private int state = PLAY_STATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);


        videoUri = MainActivity.videoUri;
        videoPlayer = (VideoView) findViewById(R.id.video_player);
        videoPlayer.setVideoURI(videoUri);

        videoLayout = (RelativeLayout) findViewById(R.id.video_layout);

        rangeSeekBar = (RangeSeekBar) findViewById(R.id.range_seek_bar);
        rangeSeekBar.setEnabled(false);

        timeBar = (TextView) findViewById(R.id.time_bar);

        videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                saveVideoInternal();
                duration = mp.getDuration() / 1000;
                mp.setLooping(true);

                currentTime = "00:00:00";
                endTime = formatTime(duration);
                timeBar.setText(currentTime + "/" + endTime);

                rangeSeekBar.setRangeValues(0, duration);
                rangeSeekBar.setEnabled(true);

                rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
                    @Override
                    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Number minValue, Number maxValue) {
                        videoPlayer.seekTo((int) minValue * 1000);
                        timeBar.setText(formatTime((int) bar.getSelectedMinValue()) + "/" + formatTime((int) bar.getSelectedMaxValue()));
                    }
                });
                final Handler handler = new Handler();
                handler.postDelayed(runnable = new Runnable() {
                    @Override
                    public void run() {

                        if (videoPlayer.getCurrentPosition() >= rangeSeekBar.getSelectedMaxValue().intValue() * 1000)
                            videoPlayer.seekTo(rangeSeekBar.getSelectedMinValue().intValue() * 1000);
                        handler.postDelayed(runnable, 1000);
                    }
                }, 1000);
            }
        });
        videoPlayer.start();

        playButton = (ImageButton) findViewById(R.id.play_video);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state % 2 == 1)
                    onResume();
                else
                    onPause();
            }
        });


        goBackButton = (ImageButton) findViewById(R.id.go_back_button);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoEditorActivity.super.onBackPressed();
            }
        });

        saveVideo = (Button) findViewById(R.id.save_video);
        saveVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveVideoExternal();
            }
        });


        cropVideoButton = (ImageButton) findViewById(R.id.crop_video);
        cropVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state < 200) {
                    state += 100;
                    videoLayout.setPadding(0, 0, 0, 200);
                    rangeSeekBar.setEnabled(true);
                    rangeSeekBar.setVisibility(View.VISIBLE);
                } else if (state >= 200 && state < 300) {
                    state -= 100;
                    videoLayout.setPadding(0, 0, 0, 100);
                    rangeSeekBar.setEnabled(false);
                    rangeSeekBar.setVisibility(View.GONE);
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
                this.finish();
                return true;
        } return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        long diff = System.currentTimeMillis();
        super.onPause();
        state += 1;
        playButton.setImageResource(R.drawable.button_play_video_white);

        diff = System.currentTimeMillis() - diff;
        stopPosition = videoPlayer.getCurrentPosition() + (int) diff;
        videoPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        state -= 1;
        playButton.setImageResource(R.drawable.button_pause_video);
        videoPlayer.seekTo(stopPosition);
        videoPlayer.start();
    }

    private void saveVideoExternal() {
        String externalStorageState = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            return;
        }

        Date now = new Date();
        int year = now.getYear();
        int month = now.getMonth();
        int day = now.getDay();
        long theTime = now.getTime();
        String time = formatTime((int) theTime);
        String result = year + "_" + month + "_" + day + "_" + time;

        String filenameExternal = "edited_videos/edited_video_" + result + ".mp4";


        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), filenameExternal);

        FileOutputStream outputStream;
        try {
            file.createNewFile();
            outputStream = new FileOutputStream(file, true);

            byte[] bytesArray = readFile(INTERNAL_STORAGE_FILENAME);

            outputStream.write(bytesArray);
            outputStream.flush();
            outputStream.close();

        } catch (Exception e) {
//            errorDialog(e.getMessage());
            e.printStackTrace();
        }
    }

    private void errorDialog(String errorMessage) {
        Dialog dialog = new Dialog(VideoEditorActivity.this);
        TextView text = new TextView(VideoEditorActivity.this);
        text.setText(errorMessage);
        dialog.setContentView(text);
        dialog.show();
    }

    private void saveVideoInternal() { saveFileToInternalStorage(INTERNAL_STORAGE_FILENAME, videoUri); }

    private byte[] readFile(Uri uri) {
        String thePath = uri.getPath();
        return readFile(thePath);
    }

    private byte[] readFile(String path) {
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(path);

            int thisLine;
            ByteArrayOutputStream bytesArrayOutputStream = new ByteArrayOutputStream();
            while ((thisLine = inputStream.read()) != -1)
                bytesArrayOutputStream.write(thisLine);

            bytesArrayOutputStream.flush();
            byte [] bytesArray = bytesArrayOutputStream.toByteArray();

            if (bytesArrayOutputStream != null)
                bytesArrayOutputStream.close();

            return bytesArray;
        } catch (Exception e) {
//            errorDialog(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void saveFileToInternalStorage(String filename, Uri uri) { saveFileToInternalStorage(filename, readFile(uri)); }

    private void saveFileToInternalStorage(String fileName, byte[] content) {
        FileOutputStream outputStream;
        try {
            outputStream = getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(content);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
//            errorDialog(e.getMessage());
            e.printStackTrace();
        }

    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int timeSec) {
        int hr = timeSec / 3600;
        int min = timeSec % 3600 / 60;
        int sec = timeSec % 60;
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }
}