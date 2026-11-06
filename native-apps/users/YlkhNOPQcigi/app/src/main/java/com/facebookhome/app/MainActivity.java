package com.facebookhome.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvUserAvatar;
    private EditText etNewPostContent;
    private Button btnPublish, btnLogout, btnSearch, btnMessenger, btnPhoto, btnLive;
    private LinearLayout storiesContainer, feedPostsContainer;
    private ProgressBar feedProgressBar;

    private BackendApi backendApi;
    private List<PostModel> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        if (backendApi.getAuthToken() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        tvUserAvatar = findViewById(R.id.tvUserAvatar);
        etNewPostContent = findViewById(R.id.etNewPostContent);
        btnPublish = findViewById(R.id.btnPublish);
        btnLogout = findViewById(R.id.btnLogout);
        btnSearch = findViewById(R.id.btnSearch);
        btnMessenger = findViewById(R.id.btnMessenger);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnLive = findViewById(R.id.btnLive);
        storiesContainer = findViewById(R.id.storiesContainer);
        feedPostsContainer = findViewById(R.id.feedPostsContainer);
        feedProgressBar = findViewById(R.id.feedProgressBar);

        postList = new ArrayList<>();

        String userEmail = backendApi.getUserEmail();
        if (userEmail != null && !userEmail.isEmpty()) {
            tvUserAvatar.setText(userEmail.substring(0, 1).toUpperCase());
        }

        setupStoriesTray();

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPost();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearAuth();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Search feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        btnMessenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Messenger inbox opening...", Toast.LENGTH_SHORT).show();
            }
        });

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Photo attachment ready", Toast.LENGTH_SHORT).show();
            }
        });

        btnLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Live Broadcast feature starting...", Toast.LENGTH_SHORT).show();
            }
        });

        fetchPosts();
    }

    private void setupStoriesTray() {
        storiesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<StoryModel> stories = new ArrayList<>();
        stories.add(new StoryModel("Add Story", "+", true));
        stories.add(new StoryModel("Sarah Jenkins", "S", false));
        stories.add(new StoryModel("Alex River", "A", false));
        stories.add(new StoryModel("Tech News", "T", false));
        stories.add(new StoryModel("David Miller", "D", false));

        for (final StoryModel story : stories) {
            View storyView = inflater.inflate(R.layout.item_story, storiesContainer, false);
            TextView tvAvatar = storyView.findViewById(R.id.tvStoryAvatar);
            TextView tvName = storyView.findViewById(R.id.tvStoryName);

            tvAvatar.setText(story.getAvatarText());
            tvName.setText(story.getName());

            if (story.isUser()) {
                tvAvatar.setBackgroundColor(getResources().getColor(R.color.fb_blue));
            } else {
                tvAvatar.setBackgroundColor(getResources().getColor(R.color.fb_dark_blue));
            }

            storyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Viewing story of " + story.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            storiesContainer.addView(storyView);
        }
    }

    private void fetchPosts() {
        feedProgressBar.setVisibility(View.VISIBLE);
        backendApi.getPosts(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                feedProgressBar.setVisibility(View.GONE);
                postList.clear();
                JSONArray records = response.optJSONArray("records");
                if (records != null) {
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject rec = records.optJSONObject(i);
                        if (rec != null) {
                            String recId = rec.optString("id", "");
                            JSONObject data = rec.optJSONObject("data");
                            if (data != null) {
                                String author = data.optString("author", "Anonymous");
                                String content = data.optString("content", "");
                                String timestamp = data.optString("timestamp", "Just now");
                                int likes = data.optInt("likes", 0);
                                String imageUrl = data.optString("image_url", "");

                                postList.add(new PostModel(recId, author, content, timestamp, likes, imageUrl));
                            }
                        }
                    }
                }

                if (postList.isEmpty()) {
                    populateDefaultSeedPosts();
                }

                renderFeedPosts();
            }

            @Override
            public void onError(String errorMessage) {
                feedProgressBar.setVisibility(View.GONE);
                populateDefaultSeedPosts();
                renderFeedPosts();
            }
        });
    }

    private void populateDefaultSeedPosts() {
        postList.add(new PostModel("seed_1", "Facebook News", "Welcome to the custom native Facebook Home Android application! Connect, share and stay updated.", "2 hours ago", 42, ""));
        postList.add(new PostModel("seed_2", "Community Updates", "Check out new mobile features and connect with your favorite groups effortlessly.", "5 hours ago", 18, ""));
    }

    private void createPost() {
        final String content = etNewPostContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please write something to post", Toast.LENGTH_SHORT).show();
            return;
        }

        String author = backendApi.getUserEmail();
        if (author.contains("@")) {
            author = author.substring(0, author.indexOf("@"));
        }

        btnPublish.setEnabled(false);
        backendApi.createPost(author, content, "", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnPublish.setEnabled(true);
                etNewPostContent.setText("");
                Toast.makeText(MainActivity.this, "Post published!", Toast.LENGTH_SHORT).show();
                fetchPosts();
            }

            @Override
            public void onError(String errorMessage) {
                btnPublish.setEnabled(true);
                Toast.makeText(MainActivity.this, "Failed to post: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderFeedPosts() {
        feedPostsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (final PostModel post : postList) {
            View postView = inflater.inflate(R.layout.item_post, feedPostsContainer, false);

            TextView tvAvatar = postView.findViewById(R.id.tvPostAvatar);
            TextView tvAuthor = postView.findViewById(R.id.tvAuthor);
            TextView tvTimestamp = postView.findViewById(R.id.tvTimestamp);
            TextView tvContent = postView.findViewById(R.id.tvContent);
            final Button btnLike = postView.findViewById(R.id.btnLike);
            Button btnComment = postView.findViewById(R.id.btnComment);
            Button btnShare = postView.findViewById(R.id.btnShare);
            Button btnDelete = postView.findViewById(R.id.btnDeletePost);

            if (post.getAuthor() != null && !post.getAuthor().isEmpty()) {
                tvAvatar.setText(post.getAuthor().substring(0, 1).toUpperCase());
            } else {
                tvAvatar.setText("F");
            }

            tvAuthor.setText(post.getAuthor());
            tvTimestamp.setText(post.getTimestamp());
            tvContent.setText(post.getContent());
            btnLike.setText("👍 Like (" + post.getLikes() + ")");

            btnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    post.setLiked(!post.isLiked());
                    int updatedLikes = post.isLiked() ? post.getLikes() + 1 : Math.max(0, post.getLikes() - 1);
                    post.setLikes(updatedLikes);
                    btnLike.setText("👍 " + (post.isLiked() ? "Liked (" : "Like (") + updatedLikes + ")");

                    if (!post.getId().startsWith("seed_")) {
                        try {
                            JSONObject updatedData = new JSONObject();
                            updatedData.put("author", post.getAuthor());
                            updatedData.put("content", post.getContent());
                            updatedData.put("timestamp", post.getTimestamp());
                            updatedData.put("likes", updatedLikes);
                            updatedData.put("image_url", post.getImageUrl());
                            backendApi.updatePostLikes(post.getId(), updatedData, new BackendApi.ApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {}

                                @Override
                                public void onError(String errorMessage) {}
                            });
                        } catch (Exception e) {}
                    }
                }
            });

            btnComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Comment section coming soon!", Toast.LENGTH_SHORT).show();
                }
            });

            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent());
                    startActivity(Intent.createChooser(shareIntent, "Share post via"));
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (post.getId().startsWith("seed_")) {
                        postList.remove(post);
                        renderFeedPosts();
                    } else {
                        backendApi.deletePost(post.getId(), new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                Toast.makeText(MainActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
                                fetchPosts();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(MainActivity.this, "Error deleting post: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });

            feedPostsContainer.addView(postView);
        }
    }
}