package com.youtubemediahub.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements VideoAdapter.OnVideoActionListener {

    private EditText editTextSearch;
    private Button buttonSearch;
    private Button buttonOpenUrl;
    private Button buttonUploadScreen;
    private Button buttonCategoryAll;
    private Button buttonCategoryMusic;
    private Button buttonCategoryGaming;
    private Button buttonCategoryTech;
    private ListView listViewVideos;

    private List<VideoItem> allVideos;
    private List<VideoItem> displayedVideos;
    private VideoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextSearch = (EditText) findViewById(R.id.editTextSearch);
        buttonSearch = (Button) findViewById(R.id.buttonSearch);
        buttonOpenUrl = (Button) findViewById(R.id.buttonOpenUrl);
        buttonUploadScreen = (Button) findViewById(R.id.buttonUploadScreen);
        buttonCategoryAll = (Button) findViewById(R.id.buttonCategoryAll);
        buttonCategoryMusic = (Button) findViewById(R.id.buttonCategoryMusic);
        buttonCategoryGaming = (Button) findViewById(R.id.buttonCategoryGaming);
        buttonCategoryTech = (Button) findViewById(R.id.buttonCategoryTech);
        listViewVideos = (ListView) findViewById(R.id.listViewVideos);

        allVideos = new ArrayList<VideoItem>();
        displayedVideos = new ArrayList<VideoItem>();

        loadSampleYouTubeData();

        adapter = new VideoAdapter(this, R.layout.list_item_video, displayedVideos, this);
        listViewVideos.setAdapter(adapter);

        filterCategory("ALL");

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonOpenUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputUrl = editTextSearch.getText().toString().trim();
                if (inputUrl.isEmpty()) {
                    inputUrl = "https://www.youtube.com";
                } else if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                    inputUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(inputUrl);
                }

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(inputUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to open link.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonUploadScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UploadActivity.class);
                startActivity(intent);
            }
        });

        buttonCategoryAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterCategory("ALL");
            }
        });

        buttonCategoryMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterCategory("Music");
            }
        });

        buttonCategoryGaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterCategory("Gaming");
            }
        });

        buttonCategoryTech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterCategory("Tech");
            }
        });
    }

    private void loadSampleYouTubeData() {
        allVideos.clear();
        allVideos.add(new VideoItem(
                "Big Buck Bunny Sample Movie",
                "Blender Foundation",
                "09:56",
                "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "Tech"
        ));
        allVideos.add(new VideoItem(
                "Elephant's Dream Open Source Video",
                "Orange Open Movie",
                "10:53",
                "https://www.youtube.com/watch?v=e-GYrbecb88",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                "Tech"
        ));
        allVideos.add(new VideoItem(
                "For Bigger Blazes Music Track",
                "Google Music Lab",
                "00:15",
                "https://www.youtube.com/watch?v=dV1w5K1eLwM",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                "Music"
        ));
        allVideos.add(new VideoItem(
                "Tears of Steel Sci-Fi Short",
                "Blender Animation",
                "12:14",
                "https://www.youtube.com/watch?v=R6MlUcmOul8",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                "Gaming"
        ));
        allVideos.add(new VideoItem(
                "Sintel Trailer & Gameplay Showcase",
                "Durian Project",
                "00:52",
                "https://www.youtube.com/watch?v=eRsGyueVLvQ",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                "Gaming"
        ));
        allVideos.add(new VideoItem(
                "Subaru Outlaw Music Sound Track",
                "Automotive Records",
                "00:30",
                "https://www.youtube.com/watch?v=1x0u-dYh9t4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                "Music"
        ));
    }

    private void filterCategory(String category) {
        displayedVideos.clear();
        if (category.equals("ALL")) {
            displayedVideos.addAll(allVideos);
        } else {
            for (int i = 0; i < allVideos.size(); i++) {
                VideoItem v = allVideos.get(i);
                if (v.getCategory().equalsIgnoreCase(category)) {
                    displayedVideos.add(v);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void performSearch() {
        String query = editTextSearch.getText().toString().trim().toLowerCase();
        displayedVideos.clear();
        if (query.isEmpty()) {
            displayedVideos.addAll(allVideos);
        } else {
            for (int i = 0; i < allVideos.size(); i++) {
                VideoItem item = allVideos.get(i);
                if (item.getTitle().toLowerCase().contains(query) ||
                    item.getChannel().toLowerCase().contains(query) ||
                    item.getCategory().toLowerCase().contains(query)) {
                    displayedVideos.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPlayClick(VideoItem item) {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        intent.putExtra("title", item.getTitle());
        intent.putExtra("channel", item.getChannel());
        intent.putExtra("youtubeUrl", item.getYoutubeUrl());
        intent.putExtra("streamUrl", item.getStreamUrl());
        startActivity(intent);
    }

    @Override
    public void onDownloadClick(VideoItem item) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(item.getStreamUrl());

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("Downloading: " + item.getTitle());
            request.setDescription("YouTube Media Hub Download");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".mp4");

            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(this, "Download started for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}