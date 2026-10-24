package com.offlinecctvrecord.app;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class VideoPlayerActivity extends Activity {

    private VideoView mVideoView;
    private TextView tvPlayerFilename;
    private TextView tvPlayerInfo;
    private Button btnPlayerBack;
    private Button btnPlayPause;
    private Button btnReplay;
    private MediaController mMediaController;

    private String videoPath = "";
    private String videoTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        videoPath = getIntent().getStringExtra("video_path");
        videoTitle = getIntent().getStringExtra("video_title");

        mVideoView = (VideoView) findViewById(R.id.video_player_view);
        tvPlayerFilename = (TextView) findViewById(R.id.tv_player_filename);
        tvPlayerInfo = (TextView) findViewById(R.id.tv_player_info);
        btnPlayerBack = (Button) findViewById(R.id.btn_player_back);
        btnPlayPause = (Button) findViewById(R.id.btn_play_pause);
        btnReplay = (Button) findViewById(R.id.btn_replay);

        if (videoTitle != null && !videoTitle.isEmpty()) {
            tvPlayerFilename.setText(videoTitle);
        }

        if (videoPath != null && !videoPath.isEmpty()) {
            File f = new File(videoPath);
            if (f.exists()) {
                tvPlayerInfo.setText("Size: " + StorageHelper.formatBytes(f.length()) + " • Pure Offline File");
            }
        }

        btnPlayerBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    btnPlayPause.setText("PLAY");
                } else {
                    mVideoView.start();
                    btnPlayPause.setText("PAUSE");
                }
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoView.seekTo(0);
                mVideoView.start();
                btnPlayPause.setText("PAUSE");
            }
        });

        setupVideoPlayback();
    }

    private void setupVideoPlayback() {
        if (videoPath == null || videoPath.isEmpty()) {
            Toast.makeText(this, "Invalid video path", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(videoPath);
        if (!file.exists()) {
            Toast.makeText(this, "Recording file does not exist", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        mVideoView.setVideoURI(Uri.fromFile(file));

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(false);
                mVideoView.start();
                btnPlayPause.setText("PAUSE");
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btnPlayPause.setText("REPLAY");
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(VideoPlayerActivity.this, "Playback error (" + what + ", " + extra + ")", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
        }
    }
}