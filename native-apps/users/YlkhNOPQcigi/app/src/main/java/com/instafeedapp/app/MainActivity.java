package com.instafeedapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private BackendApi backendApi;
    private LinearLayout containerStories;
    private LinearLayout containerFeed;
    private TextView tvEmptyState;
    private Button btnAuthAction;
    private Button btnAddPost;

    private final List<PostItem> postList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        containerStories = findViewById(R.id.container_stories);
        containerFeed = findViewById(R.id.container_feed);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        btnAuthAction = findViewById(R.id.btn_auth_action);
        btnAddPost = findViewById(R.id.btn_add_post);

        setupAuthButtonState();
        populateStories();
        loadPostsFromBackend();

        btnAuthAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backendApi.isLoggedIn()) {
                    backendApi.logout();
                    setupAuthButtonState();
                    Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                } else {
                    showAuthDialog();
                }
            }
        });

        btnAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPostDialog();
            }
        });
    }

    private void setupAuthButtonState() {
        if (backendApi.isLoggedIn()) {
            btnAuthAction.setText(R.string.logout);
        } else {
            btnAuthAction.setText(R.string.login);
        }
    }

    private void populateStories() {
        containerStories.removeAllViews();
        String[] usernames = {"your_story", "alex_photos", "travel_bug", "nature_shots", "city_vibes", "art_daily"};
        
        for (int i = 0; i < usernames.length; i++) {
            final String name = usernames[i];
            View storyView = LayoutInflater.from(this).inflate(R.layout.item_post, containerStories, false);
            
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(12, 0, 12, 0);

            TextView avatar = new TextView(this);
            avatar.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
            avatar.setBackgroundColor(i == 0 ? 0xFF833AB4 : 0xFFE1306C);
            avatar.setTextColor(0xFFFFFFFF);
            avatar.setText(name.substring(0, 1).toUpperCase());
            avatar.setTextSize(18);
            avatar.setGravity(android.view.Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(name);
            label.setTextSize(11);
            label.setTextColor(0xFF262626);
            label.setGravity(android.view.Gravity.CENTER);
            label.setPadding(0, 8, 0, 0);

            itemLayout.addView(avatar);
            itemLayout.addView(label);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Viewing story by " + name, Toast.LENGTH_SHORT).show();
                }
            });

            containerStories.addView(itemLayout);
        }
    }

    private void loadPostsFromBackend() {
        backendApi.fetchPosts(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(final JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        postList.clear();
                        JSONArray records = response.optJSONArray("records");
                        if (records != null && records.length() > 0) {
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject rec = records.optJSONObject(i);
                                if (rec != null) {
                                    String id = rec.optString("id", "");
                                    JSONObject data = rec.optJSONObject("data");
                                    if (data != null) {
                                        String username = data.optString("username", "anonymous");
                                        String caption = data.optString("caption", "");
                                        String imageUrl = data.optString("image_url", "");
                                        String location = data.optString("location", "");
                                        int likes = data.optInt("likes", 0);

                                        postList.add(new PostItem(id, username, caption, imageUrl, location, likes));
                                    }
                                }
                            }
                        }
                        
                        if (postList.isEmpty()) {
                            loadSampleDemoData();
                        } else {
                            renderFeed();
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadSampleDemoData();
                    }
                });
            }
        });
    }

    private void loadSampleDemoData() {
        postList.clear();
        postList.add(new PostItem("demo_1", "explore_world", "Exploring the sunset views over the coastal cliffs! 🌅✨ #travel #vibes", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800", "Santorini, Greece", 142));
        postList.add(new PostItem("demo_2", "urban_architect", "Clean minimalism in modern city architecture. 🏢", "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800", "Tokyo, Japan", 89));
        postList.add(new PostItem("demo_3", "coffeelover", "Morning coffee routine to kickstart the day. ☕️", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800", "Paris, France", 254));
        renderFeed();
    }

    private void renderFeed() {
        containerFeed.removeAllViews();
        if (postList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyState.setVisibility(View.GONE);

        for (final PostItem post : postList) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_post, containerFeed, false);

            TextView tvAvatar = itemView.findViewById(R.id.tv_avatar_placeholder);
            TextView tvUsername = itemView.findViewById(R.id.tv_post_username);
            TextView tvLocation = itemView.findViewById(R.id.tv_post_location);
            ImageView ivImage = itemView.findViewById(R.id.iv_post_image);
            final Button btnLike = itemView.findViewById(R.id.btn_like);
            Button btnComment = itemView.findViewById(R.id.btn_comment);
            final Button btnBookmark = itemView.findViewById(R.id.btn_bookmark);
            final TextView tvLikesCount = itemView.findViewById(R.id.tv_likes_count);
            TextView tvCaption = itemView.findViewById(R.id.tv_caption);

            tvAvatar.setText(post.getUsername().substring(0, 1).toUpperCase());
            tvUsername.setText(post.getUsername());
            tvLocation.setText(post.getLocation().isEmpty() ? "Sponsored" : post.getLocation());
            tvLikesCount.setText(post.getLikes() + " likes");
            tvCaption.setText(post.getUsername() + "  " + post.getCaption());

            ImageLoader.getInstance().loadImage(post.getImageUrl(), ivImage);

            btnLike.setTextColor(post.isLiked() ? 0xFFED4956 : 0xFF262626);
            btnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean nowLiked = !post.isLiked();
                    post.setLiked(nowLiked);
                    int currentLikes = post.getLikes();
                    int updatedLikes = nowLiked ? currentLikes + 1 : Math.max(0, currentLikes - 1);
                    post.setLikes(updatedLikes);

                    tvLikesCount.setText(updatedLikes + " likes");
                    btnLike.setTextColor(nowLiked ? 0xFFED4956 : 0xFF262626);

                    try {
                        JSONObject updatedFields = new JSONObject();
                        updatedFields.put("username", post.getUsername());
                        updatedFields.put("caption", post.getCaption());
                        updatedFields.put("image_url", post.getImageUrl());
                        updatedFields.put("location", post.getLocation());
                        updatedFields.put("likes", updatedLikes);

                        backendApi.updateLikes(post.getId(), updatedFields, new BackendApi.ApiCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {}

                            @Override
                            public void onError(String error) {}
                        });
                    } catch (Exception ignored) {}
                }
            });

            btnComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Comments feature opened for post", Toast.LENGTH_SHORT).show();
                }
            });

            btnBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean bookmarked = !post.isBookmarked();
                    post.setBookmarked(bookmarked);
                    btnBookmark.setTextColor(bookmarked ? 0xFF0095F6 : 0xFF262626);
                    Toast.makeText(MainActivity.this, bookmarked ? "Saved to Bookmarks" : "Removed from Bookmarks", Toast.LENGTH_SHORT).show();
                }
            });

            containerFeed.addView(itemView);
        }
    }

    private void showAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        final EditText etEmail = view.findViewById(R.id.et_email);
        final EditText etPassword = view.findViewById(R.id.et_password);
        Button btnSubmitLogin = view.findViewById(R.id.btn_submit_login);
        Button btnSubmitRegister = view.findViewById(R.id.btn_submit_register);

        btnSubmitLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                backendApi.login(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupAuthButtonState();
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        btnSubmitRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter email & password to register", Toast.LENGTH_SHORT).show();
                    return;
                }

                backendApi.register(email, pass, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupAuthButtonState();
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Register error: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void showAddPostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_post, null);
        builder.setView(view);

        final AlertDialog dialog = builder.create();

        final EditText etImageUrl = view.findViewById(R.id.et_image_url);
        final EditText etCaption = view.findViewById(R.id.et_caption);
        final EditText etLocation = view.findViewById(R.id.et_location);
        Button btnCancel = view.findViewById(R.id.btn_cancel_post);
        Button btnPublish = view.findViewById(R.id.btn_publish_post);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String imageUrl = etImageUrl.getText().toString().trim();
                String caption = etCaption.getText().toString().trim();
                String location = etLocation.getText().toString().trim();

                if (imageUrl.isEmpty() || caption.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please provide image URL and caption", Toast.LENGTH_SHORT).show();
                    return;
                }

                String username = backendApi.isLoggedIn() ? "my_profile" : "creator_user";

                backendApi.createPost(username, caption, imageUrl, location, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Post published!", Toast.LENGTH_SHORT).show();
                                loadPostsFromBackend();
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postList.add(0, new PostItem("local_" + System.currentTimeMillis(), username, caption, imageUrl, location, 0));
                                renderFeed();
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Post added locally!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }
}