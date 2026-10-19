package com.arbajaistudio.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class VideoPlayerActivity extends Activity {

    private VideoView videoView;
    private ProgressBar pbBuffering;
    private TextView tvPlayerTitle;
    private TextView tvPlayerError;
    private TextView tvVideoMetaInfo;
    private Button btnBack;
    private Button btnDownload;
    private Button btnPlayPause;
    private Button btnReplay;

    private String videoUrl;
    private String videoTitle;
    private String videoMeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoUrl = getIntent().getStringExtra("video_url");
        videoTitle = getIntent().getStringExtra("video_title");
        videoMeta = getIntent().getStringExtra("video_meta");

        if (videoTitle == null || videoTitle.isEmpty()) {
            videoTitle = "AI Generated Video";
        }

        videoView = (VideoView) findViewById(R.id.video_view);
        pbBuffering = (ProgressBar) findViewById(R.id.pb_video_buffering);
        tvPlayerTitle = (TextView) findViewById(R.id.tv_player_title);
        tvPlayerError = (TextView) findViewById(R.id.tv_player_error);
        tvVideoMetaInfo = (TextView) findViewById(R.id.tv_video_meta_info);
        btnBack = (Button) findViewById(R.id.btn_back_player);
        btnDownload = (Button) findViewById(R.id.btn_download_video);
        btnPlayPause = (Button) findViewById(R.id.btn_play_pause);
        btnReplay = (Button) findViewById(R.id.btn_replay);

        tvPlayerTitle.setText(videoTitle);
        if (videoMeta != null) {
            tvVideoMetaInfo.setText(videoMeta);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadVideoFile();
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPlayPause.setText("Play");
                } else {
                    videoView.start();
                    btnPlayPause.setText("Pause");
                }
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.seekTo(0);
                videoView.start();
                btnPlayPause.setText("Pause");
            }
        });

        setupPlayer();
    }

    private void setupPlayer() {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            pbBuffering.setVisibility(View.GONE);
            tvPlayerError.setText("Error: Missing video stream URL.");
            tvPlayerError.setVisibility(View.VISIBLE);
            return;
        }

        pbBuffering.setVisibility(View.VISIBLE);
        tvPlayerError.setVisibility(View.GONE);

        try {
            Uri uri = Uri.parse(videoUrl);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    pbBuffering.setVisibility(View.GONE);
                    mp.setLooping(true);
                    videoView.start();
                    btnPlayPause.setText("Pause");
                }
            });

            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    pbBuffering.setVisibility(View.GONE);
                    tvPlayerError.setText("Playback failed (Code: " + what + ", " + extra + ").\nTap 'Download MP4' to save and play in gallery.");
                    tvPlayerError.setVisibility(View.VISIBLE);
                    return true;
                }
            });

        } catch (Exception e) {
            pbBuffering.setVisibility(View.GONE);
            tvPlayerError.setText("Failed to initialize player: " + e.getMessage());
            tvPlayerError.setVisibility(View.VISIBLE);
        }
    }

    private void downloadVideoFile() {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "No video URL to download", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri downloadUri = Uri.parse(videoUrl);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setTitle(videoTitle);
            request.setDescription("Downloading genuine AI generated video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            String filename = "ArbajAI_" + System.currentTimeMillis() + ".mp4";
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, filename);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Downloading to Movies/" + filename, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Download service unavailable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
    }
}