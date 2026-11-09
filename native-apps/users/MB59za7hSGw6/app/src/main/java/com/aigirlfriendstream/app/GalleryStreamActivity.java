package com.aigirlfriendstream.app;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class GalleryStreamActivity extends Activity {

    private static final int REQUEST_PICK_VIDEO = 201;

    private VideoView videoView;
    private TextView tvEmptyState;
    private Button btnMuteToggle;
    private Button btnPlayPause;
    private MediaPlayer mediaPlayerInstance;
    private boolean isMuted = false;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_stream);

        videoView = (VideoView) findViewById(R.id.videoView);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        btnMuteToggle = (Button) findViewById(R.id.btnMuteToggle);
        btnPlayPause = (Button) findViewById(R.id.btnPlayPause);

        Button btnPickVideo = (Button) findViewById(R.id.btnPickVideo);

        btnPickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(Intent.createChooser(intent, "Select Video Stream"), REQUEST_PICK_VIDEO);
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    isPlaying = false;
                    btnPlayPause.setText("Play");
                } else {
                    videoView.start();
                    isPlaying = true;
                    btnPlayPause.setText("Pause");
                }
            }
        });

        btnMuteToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMuted = !isMuted;
                if (mediaPlayerInstance != null) {
                    if (isMuted) {
                        mediaPlayerInstance.setVolume(0f, 0f);
                        btnMuteToggle.setText("UNMUTE");
                    } else {
                        mediaPlayerInstance.setVolume(1f, 1f);
                        btnMuteToggle.setText("MUTE");
                    }
                }
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayerInstance = mp;
                mp.setLooping(true);
                if (isMuted) {
                    mp.setVolume(0f, 0f);
                } else {
                    mp.setVolume(1f, 1f);
                }
                tvEmptyState.setVisibility(View.GONE);
                videoView.start();
                isPlaying = true;
                btnPlayPause.setText("Pause");
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(GalleryStreamActivity.this, "Error playing video stream", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                videoView.setVideoURI(videoUri);
                tvEmptyState.setText("Loading video stream...");
            }
        }
    }
}