package com.socialconnect.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity {

    private SessionManager sessionManager;
    private BackendApi backendApi;

    private TextView tvUserAvatar, btnTogglePhoto, btnRefresh;
    private EditText etNewPost, etImageUrl;
    private Button btnPost, btnWebShortcut, btnLogout;
    private LinearLayout feedContainer;
    private ProgressBar feedProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        tvUserAvatar = (TextView) findViewById(R.id.tvUserAvatar);
        etNewPost = (EditText) findViewById(R.id.etNewPost);
        etImageUrl = (EditText) findViewById(R.id.etImageUrl);
        btnPost = (Button) findViewById(R.id.btnPost);
        btnWebShortcut = (Button) findViewById(R.id.btnWebShortcut);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnTogglePhoto = (TextView) findViewById(R.id.btnTogglePhoto);
        btnRefresh = (TextView) findViewById(R.id.btnRefresh);

        feedContainer = (LinearLayout) findViewById(R.id.feedContainer);
        feedProgress = (ProgressBar) findViewById(R.id.feedProgress);

        String userName = sessionManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvUserAvatar.setText(userName.substring(0, 1).toUpperCase());
        }

        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewPost();
            }
        });

        btnTogglePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etImageUrl.getVisibility() == View.GONE) {
                    etImageUrl.setVisibility(View.VISIBLE);
                } else {
                    etImageUrl.setVisibility(View.GONE);
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFeedPosts();
            }
        });

        btnWebShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"));
                startActivity(browserIntent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionManager.clearSession();
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        loadFeedPosts();
    }

    private void createNewPost() {
        String content = etNewPost.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "Please enter some text to post", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        backendApi.createPost(content, imageUrl, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                btnPost.setEnabled(true);
                etNewPost.setText("");
                etImageUrl.setText("");
                etImageUrl.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Post created successfully!", Toast.LENGTH_SHORT).show();
                loadFeedPosts();
            }

            @Override
            public void onError(String errorMessage) {
                btnPost.setEnabled(true);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadFeedPosts() {
        feedProgress.setVisibility(View.VISIBLE);
        feedContainer.removeAllViews();

        backendApi.fetchPosts(new BackendApi.PostsCallback() {
            @Override
            public void onSuccess(List<PostModel> posts) {
                feedProgress.setVisibility(View.GONE);
                if (posts.isEmpty()) {
                    TextView tvEmpty = new TextView(MainActivity.this);
                    tvEmpty.setText("No posts yet. Write the first update above!");
                    tvEmpty.setPadding(32, 64, 32, 64);
                    tvEmpty.setTextSize(14);
                    tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
                    feedContainer.addView(tvEmpty);

                    // Add welcome example post locally if empty
                    addWelcomePost();
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                for (final PostModel post : posts) {
                    View itemView = inflater.inflate(R.layout.item_post, feedContainer, false);

                    TextView tvAvatar = (TextView) itemView.findViewById(R.id.tvAvatar);
                    TextView tvAuthorName = (TextView) itemView.findViewById(R.id.tvAuthorName);
                    TextView tvPostTime = (TextView) itemView.findViewById(R.id.tvPostTime);
                    TextView tvPostContent = (TextView) itemView.findViewById(R.id.tvPostContent);
                    TextView tvImagePlaceholder = (TextView) itemView.findViewById(R.id.tvImagePlaceholder);
                    final TextView tvLikeText = (TextView) itemView.findViewById(R.id.tvLikeText);
                    LinearLayout btnLike = (LinearLayout) itemView.findViewById(R.id.btnLike);

                    String author = post.getAuthorName();
                    if (author != null && !author.isEmpty()) {
                        tvAvatar.setText(author.substring(0, 1).toUpperCase());
                        tvAuthorName.setText(author);
                    }

                    tvPostTime.setText(SocialLogicHelper.formatTimeAgo(post.getTimestamp()));
                    tvPostContent.setText(post.getContent());

                    if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
                        tvImagePlaceholder.setVisibility(View.VISIBLE);
                        tvImagePlaceholder.setText("📷 Attachment: " + post.getImageUrl());
                    } else {
                        tvImagePlaceholder.setVisibility(View.GONE);
                    }

                    tvLikeText.setText(SocialLogicHelper.formatLikesCount(post.getLikes()));

                    btnLike.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int newLikes = post.getLikes() + 1;
                            post.setLikes(newLikes);
                            tvLikeText.setText(SocialLogicHelper.formatLikesCount(newLikes));

                            try {
                                JSONObject updatedData = new JSONObject();
                                updatedData.put("author_name", post.getAuthorName());
                                updatedData.put("author_email", post.getAuthorEmail());
                                updatedData.put("content", post.getContent());
                                updatedData.put("image_url", post.getImageUrl());
                                updatedData.put("likes", newLikes);
                                updatedData.put("timestamp", post.getTimestamp());

                                backendApi.updatePostLikes(post.getId(), updatedData, new BackendApi.ApiCallback() {
                                    @Override
                                    public void onSuccess(JSONObject response) {
                                        // Liked updated remotely
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        // Ignore transient errors
                                    }
                                });
                            } catch (Exception e) {
                                // Safe handling
                            }
                        }
                    });

                    feedContainer.addView(itemView);
                }
            }

            @Override
            public void onError(String errorMessage) {
                feedProgress.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                addWelcomePost();
            }
        });
    }

    private void addWelcomePost() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View itemView = inflater.inflate(R.layout.item_post, feedContainer, false);

        TextView tvAvatar = (TextView) itemView.findViewById(R.id.tvAvatar);
        TextView tvAuthorName = (TextView) itemView.findViewById(R.id.tvAuthorName);
        TextView tvPostTime = (TextView) itemView.findViewById(R.id.tvPostTime);
        TextView tvPostContent = (TextView) itemView.findViewById(R.id.tvPostContent);
        final TextView tvLikeText = (TextView) itemView.findViewById(R.id.tvLikeText);
        LinearLayout btnLike = (LinearLayout) itemView.findViewById(R.id.btnLike);

        tvAvatar.setText("F");
        tvAuthorName.setText("Social Connect Team");
        tvPostTime.setText("Just now");
        tvPostContent.setText("Welcome to Social Connect! Share status updates with friends, or click 'Web Site' at top to access Facebook on the web.");
        tvLikeText.setText("👍 Like");

        btnLike.setOnClickListener(new View.OnClickListener() {
            private int likes = 0;
            @Override
            public void onClick(View v) {
                likes++;
                tvLikeText.setText(SocialLogicHelper.formatLikesCount(likes));
            }
        });

        feedContainer.addView(itemView);
    }
}