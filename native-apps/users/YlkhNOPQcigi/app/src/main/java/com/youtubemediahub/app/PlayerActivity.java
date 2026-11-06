package com.youtubemediahub.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayerActivity extends Activity {

    private VideoView videoView;
    private ProgressBar progressBar;
    private TextView textViewVideoTitle;
    private TextView textViewChannelName;
    private TextView textViewVideoUrl;
    private Button buttonOpenExternal;
    private Button buttonBack;

    private String videoTitle;
    private String channelName;
    private String youtubeUrl;
    private String streamUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoView = (VideoView) findViewById(R.id.videoView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textViewVideoTitle = (TextView) findViewById(R.id.textViewVideoTitle);
        textViewChannelName = (TextView) findViewById(R.id.textViewChannelName);
        textViewVideoUrl = (TextView) findViewById(R.id.textViewVideoUrl);
        buttonOpenExternal = (Button) findViewById(R.id.buttonOpenExternal);
        buttonBack = (Button) findViewById(R.id.buttonBack);

        Intent intent = getIntent();
        if (intent != null) {
            videoTitle = intent.getStringExtra("title");
            channelName = intent.getStringExtra("channel");
            youtubeUrl = intent.getStringExtra("youtubeUrl");
            streamUrl = intent.getStringExtra("streamUrl");
        }

        if (videoTitle == null) videoTitle = "YouTube Video";
        if (channelName == null) channelName = "YouTube Channel";
        if (youtubeUrl == null) youtubeUrl = "https://www.youtube.com";

        textViewVideoTitle.setText(videoTitle);
        textViewChannelName.setText(channelName);
        textViewVideoUrl.setText(youtubeUrl);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        if (streamUrl != null && !streamUrl.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            Uri videoUri = Uri.parse(streamUrl);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(android.media.MediaPlayer mediaPlayer) {
                    progressBar.setVisibility(View.GONE);
                    videoView.start();
                }
            });

            videoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(android.media.MediaPlayer mediaPlayer, int what, int extra) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Stream playback failed. Open via YouTube below.", Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Direct stream unavailable. Click 'Watch on YouTube App/Browser'", Toast.LENGTH_SHORT).show();
        }

        buttonOpenExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl));
                    startActivity(launchIntent);
                } catch (Exception e) {
                    Toast.makeText(PlayerActivity.this, "Cannot open video link.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}