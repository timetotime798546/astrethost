package com.facebookhome.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

    private BackendApi backendApi;
    private TextView tvUserAvatar, tabHome, tabNotifications, tabProfile;
    private EditText etNewPost;
    private CheckBox cbAttachPhoto;
    private Button btnPost, btnLogout, btnWebPortalHeader, btnLaunchWebBanner;
    private LinearLayout containerPosts, containerStories;
    private ProgressBar pbFeed;

    private List<PostModel> postList;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);
        if (backendApi.getAuthToken() == null) {
            startLoginActivity();
            finish();
            return;
        }

        currentUserEmail = backendApi.getUserEmail();
        postList = new ArrayList<>();

        tvUserAvatar = findViewById(R.id.tvUserAvatar);
        etNewPost = findViewById(R.id.etNewPost);
        cbAttachPhoto = findViewById(R.id.cbAttachPhoto);
        btnPost = findViewById(R.id.btnPost);
        btnLogout = findViewById(R.id.btnLogout);
        btnWebPortalHeader = findViewById(R.id.btnWebPortalHeader);
        btnLaunchWebBanner = findViewById(R.id.btnLaunchWebBanner);

        tabHome = findViewById(R.id.tabHome);
        tabNotifications = findViewById(R.id.tabNotifications);
        tabProfile = findViewById(R.id.tabProfile);

        containerPosts = findViewById(R.id.containerPosts);
        containerStories = findViewById(R.id.containerStories);
        pbFeed = findViewById(R.id.pbFeed);

        if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
            tvUserAvatar.setText(currentUserEmail.substring(0, 1).toUpperCase());
        }

        setupTabListeners();
        setupStories();
        fetchFeedPosts();

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewPost();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearAuth();
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                startLoginActivity();
                finish();
            }
        });

        btnWebPortalHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFacebookWeb();
            }
        });

        btnLaunchWebBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFacebookWeb();
            }
        });
    }

    private void openFacebookWeb() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"));
        startActivity(browserIntent);
    }

    private void setupTabListeners() {
        tabHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightTab(tabHome);
                fetchFeedPosts();
            }
        });

        tabNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightTab(tabNotifications);
                showNotificationsDemo();
            }
        });

        tabProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightTab(tabProfile);
                showProfilePosts();
            }
        });
    }

    private void highlightTab(TextView activeTab) {
        tabHome.setTextColor(getResources().getColor(R.color.fb_text_secondary));
        tabNotifications.setTextColor(getResources().getColor(R.color.fb_text_secondary));
        tabProfile.setTextColor(getResources().getColor(R.color.fb_text_secondary));

        activeTab.setTextColor(getResources().getColor(R.color.fb_blue));
    }

    private void setupStories() {
        containerStories.removeAllViews();
        String[] storyUsers = {"Create Story", "Alex Johnson", "Sarah Connor", "Tech Daily", "Mark S."};

        for (int i = 0; i < storyUsers.length; i++) {
            View storyView = LayoutInflater.from(this).inflate(R.layout.item_story, containerStories, false);
            TextView tvStoryAvatar = storyView.findViewById(R.id.tvStoryAvatar);
            TextView tvStoryName = storyView.findViewById(R.id.tvStoryName);

            String name = storyUsers[i];
            tvStoryName.setText(name);
            tvStoryAvatar.setText(name.substring(0, 1).toUpperCase());

            storyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Viewing Story of " + name, Toast.LENGTH_SHORT).show();
                }
            });

            containerStories.addView(storyView);
        }
    }

    private void createNewPost() {
        final String content = etNewPost.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter some text to post", Toast.LENGTH_SHORT).show();
            return;
        }

        final String imageUrl = cbAttachPhoto.isChecked() ? "https://example.com/photo.jpg" : "";

        pbFeed.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);

        backendApi.createPost(currentUserEmail, content, imageUrl, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbFeed.setVisibility(View.GONE);
                btnPost.setEnabled(true);
                etNewPost.setText("");
                cbAttachPhoto.setChecked(false);
                Toast.makeText(MainActivity.this, "Posted to your timeline!", Toast.LENGTH_SHORT).show();
                fetchFeedPosts();
            }

            @Override
            public void onError(String errorMessage) {
                pbFeed.setVisibility(View.GONE);
                btnPost.setEnabled(true);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchFeedPosts() {
        pbFeed.setVisibility(View.VISIBLE);
        backendApi.getPosts(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbFeed.setVisibility(View.GONE);
                postList.clear();

                try {
                    JSONArray records = response.optJSONArray("records");
                    if (records != null) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject rec = records.getJSONObject(i);
                            String recordId = rec.optString("id");
                            JSONObject data = rec.optJSONObject("data");

                            if (data != null) {
                                String author = data.optString("author", "Anonymous");
                                String content = data.optString("content", "");
                                String timestamp = data.optString("timestamp", "Recent");
                                int likes = data.optInt("likes", 0);
                                String imageUrl = data.optString("image_url", "");

                                postList.add(new PostModel(recordId, author, content, timestamp, likes, imageUrl));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Add default sample posts if server list is empty
                if (postList.isEmpty()) {
                    postList.add(new PostModel("demo1", "Facebook Home Updates", "Welcome to Facebook Home Native App! Connect with your friends, share updates, and access the web portal.", "10 mins ago", 12, ""));
                    postList.add(new PostModel("demo2", "Community News", "Stay connected with real-time posts, timeline updates, and direct links to facebook.com.", "1 hr ago", 45, "photo"));
                }

                renderFeedView();
            }

            @Override
            public void onError(String errorMessage) {
                pbFeed.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Loaded cached feed", Toast.LENGTH_SHORT).show();
                if (postList.isEmpty()) {
                    postList.add(new PostModel("demo1", "Facebook Home Updates", "Welcome to Facebook Home Native App! Connect with your friends, share updates, and access the web portal.", "10 mins ago", 12, ""));
                }
                renderFeedView();
            }
        });
    }

    private void showNotificationsDemo() {
        containerPosts.removeAllViews();
        String[] sampleAlerts = {
            "🔔 Alex liked your recent status update.",
            "🔔 Sarah commented on your photo.",
            "🔔 Facebook Home: Security check completed.",
            "🔔 Your friend request was accepted."
        };

        for (int i = 0; i < sampleAlerts.length; i++) {
            TextView tvAlert = new TextView(this);
            tvAlert.setText(sampleAlerts[i]);
            tvAlert.setTextColor(getResources().getColor(R.color.fb_text_primary));
            tvAlert.setTextSize(14);
            tvAlert.setPadding(24, 24, 24, 24);
            tvAlert.setBackgroundColor(getResources().getColor(R.color.fb_card_bg));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            tvAlert.setLayoutParams(lp);
            containerPosts.addView(tvAlert);
        }
    }

    private void showProfilePosts() {
        containerPosts.removeAllViews();

        TextView tvHeader = new TextView(this);
        tvHeader.setText("👤 Your Profile Posts (" + currentUserEmail + ")");
        tvHeader.setTextColor(getResources().getColor(R.color.fb_text_primary));
        tvHeader.setTextSize(16);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setPadding(16, 16, 16, 16);
        containerPosts.addView(tvHeader);

        int count = 0;
        for (int i = 0; i < postList.size(); i++) {
            PostModel post = postList.get(i);
            if (post.getAuthor().equalsIgnoreCase(currentUserEmail)) {
                renderSinglePostView(post);
                count++;
            }
        }

        if (count == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("You haven't created any posts yet. Use the top compose box!");
            tvEmpty.setTextColor(getResources().getColor(R.color.fb_text_secondary));
            tvEmpty.setPadding(24, 24, 24, 24);
            containerPosts.addView(tvEmpty);
        }
    }

    private void renderFeedView() {
        containerPosts.removeAllViews();
        for (int i = 0; i < postList.size(); i++) {
            renderSinglePostView(postList.get(i));
        }
    }

    private void renderSinglePostView(final PostModel post) {
        View postView = LayoutInflater.from(this).inflate(R.layout.item_post, containerPosts, false);

        TextView tvPostAvatar = postView.findViewById(R.id.tvPostAvatar);
        TextView tvAuthor = postView.findViewById(R.id.tvAuthor);
        TextView tvTimestamp = postView.findViewById(R.id.tvTimestamp);
        TextView tvContent = postView.findViewById(R.id.tvContent);
        LinearLayout imageContainer = postView.findViewById(R.id.imageContainer);

        final Button btnLike = postView.findViewById(R.id.btnLike);
        Button btnComment = postView.findViewById(R.id.btnComment);
        Button btnShare = postView.findViewById(R.id.btnShare);
        Button btnDeletePost = postView.findViewById(R.id.btnDeletePost);

        String author = post.getAuthor();
        tvAuthor.setText(author);
        tvTimestamp.setText(post.getTimestamp());
        tvContent.setText(post.getContent());

        if (author != null && !author.isEmpty()) {
            tvPostAvatar.setText(author.substring(0, 1).toUpperCase());
        } else {
            tvPostAvatar.setText("F");
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        updateLikeButtonState(btnLike, post);

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post.setLiked(!post.isLiked());
                int newLikes = post.isLiked() ? post.getLikes() + 1 : Math.max(0, post.getLikes() - 1);
                post.setLikes(newLikes);
                updateLikeButtonState(btnLike, post);

                if (!post.getId().startsWith("demo")) {
                    try {
                        JSONObject updatedData = new JSONObject();
                        updatedData.put("author", post.getAuthor());
                        updatedData.put("content", post.getContent());
                        updatedData.put("timestamp", post.getTimestamp());
                        updatedData.put("likes", newLikes);
                        updatedData.put("image_url", post.getImageUrl());
                        backendApi.updatePostLikes(post.getId(), updatedData, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {}
                            @Override
                            public void onError(String errorMessage) {}
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Opening comments for " + post.getAuthor() + "'s post", Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent() + " - via Facebook Home");
                startActivity(Intent.createChooser(shareIntent, "Share Post"));
            }
        });

        btnDeletePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (post.getId().startsWith("demo")) {
                    postList.remove(post);
                    renderFeedView();
                } else {
                    backendApi.deletePost(post.getId(), new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Toast.makeText(MainActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
                            fetchFeedPosts();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        containerPosts.addView(postView);
    }

    private void updateLikeButtonState(Button btnLike, PostModel post) {
        if (post.isLiked()) {
            btnLike.setText("👍 Liked (" + post.getLikes() + ")");
            btnLike.setTextColor(getResources().getColor(R.color.fb_like_blue));
        } else {
            btnLike.setText("👍 Like (" + post.getLikes() + ")");
            btnLike.setTextColor(getResources().getColor(R.color.fb_text_secondary));
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}