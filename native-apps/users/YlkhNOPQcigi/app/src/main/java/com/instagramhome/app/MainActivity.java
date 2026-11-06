package com.instagramhome.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String INSTAGRAM_URL = "https://www.instagram.com";

    private LinearLayout storiesContainer;
    private LinearLayout postsContainer;
    private Handler mainHandler;

    private static class PostModel {
        String username;
        String userAvatarUrl;
        String postImageUrl;
        String caption;
        int likesCount;
        boolean isLiked;

        PostModel(String username, String userAvatarUrl, String postImageUrl, String caption, int likesCount) {
            this.username = username;
            this.userAvatarUrl = userAvatarUrl;
            this.postImageUrl = postImageUrl;
            this.caption = caption;
            this.likesCount = likesCount;
            this.isLiked = false;
        }
    }

    private static class StoryModel {
        String name;
        String avatarUrl;

        StoryModel(String name, String avatarUrl) {
            this.name = name;
            this.avatarUrl = avatarUrl;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        storiesContainer = findViewById(R.id.stories_container);
        postsContainer = findViewById(R.id.posts_container);

        Button btnOpenWeb = findViewById(R.id.btn_open_instagram_web);
        Button btnBannerLaunch = findViewById(R.id.btn_banner_launch);
        TextView navWeb = findViewById(R.id.nav_web);
        TextView navExplore = findViewById(R.id.nav_explore);

        View.OnClickListener launchInstagramListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInstagramWeb();
            }
        };

        btnOpenWeb.setOnClickListener(launchInstagramListener);
        btnBannerLaunch.setOnClickListener(launchInstagramListener);
        navWeb.setOnClickListener(launchInstagramListener);

        navExplore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.getApplicationContext(), "Explore section loaded", Toast.LENGTH_SHORT).show();
            }
        });

        loadStoriesTray();
        loadInstagramFeed();
    }

    private void openInstagramWeb() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open browser for Instagram", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStoriesTray() {
        List<StoryModel> stories = new ArrayList<>();
        stories.add(new StoryModel("Your Story", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"));
        stories.add(new StoryModel("alex_photo", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"));
        stories.add(new StoryModel("travel_life", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"));
        stories.add(new StoryModel("design_hub", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"));
        stories.add(new StoryModel("fitness_pro", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150"));

        for (int i = 0; i < stories.size(); i++) {
            final StoryModel story = stories.get(i);

            LinearLayout storyLayout = new LinearLayout(this);
            storyLayout.setOrientation(LinearLayout.VERTICAL);
            storyLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(160, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(12, 0, 12, 0);
            storyLayout.setLayoutParams(lp);

            final ImageView avatarView = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(130, 130);
            avatarView.setLayoutParams(imgLp);
            avatarView.setBackgroundColor(Color.LTGRAY);
            avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            TextView nameView = new TextView(this);
            nameView.setText(story.name);
            nameView.setTextSize(11sp);
            nameView.setTextColor(Color.parseColor("#262626"));
            nameView.setGravity(Gravity.CENTER);
            nameView.setSingleLine(true);
            nameView.setPadding(0, 8, 0, 0);

            storyLayout.addView(avatarView);
            storyLayout.addView(nameView);

            storyLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Viewing " + story.name + "'s story", Toast.LENGTH_SHORT).show();
                }
            });

            storiesContainer.addView(storyLayout);

            loadImageFromUrl(story.avatarUrl, avatarView);
        }
    }

    private void loadInstagramFeed() {
        List<PostModel> posts = new ArrayList<>();

        posts.add(new PostModel(
                "nature_explorer",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800",
                "Sunset over the misty mountains! Absolute peace. 🌄 #nature #sunset #photography",
                1240
        ));

        posts.add(new PostModel(
                "urban_vibes",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800",
                "Exploring downtown architecture on a rainy evening. 🏙️✨ #city #urban #architecture",
                895
        ));

        posts.add(new PostModel(
                "coffee_lover",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=800",
                "Morning ritual: Fresh roast coffee & fresh thoughts. ☕📖 #coffee #morning",
                342
        ));

        for (int i = 0; i < posts.size(); i++) {
            final PostModel post = posts.get(i);
            addPostToFeed(post);
        }
    }

    private void addPostToFeed(final PostModel post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cardLp);

        // Header (Avatar + Username)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(24, 16, 24, 16);

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(80, 80);
        avatar.setLayoutParams(avLp);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundColor(Color.LTGRAY);

        TextView userTxt = new TextView(this);
        userTxt.setText(post.username);
        userTxt.setTextSize(14sp);
        userTxt.setTypeface(null, android.graphics.Typeface.BOLD);
        userTxt.setTextColor(Color.parseColor("#262626"));
        LinearLayout.LayoutParams userLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        userLp.setMargins(20, 0, 0, 0);
        userTxt.setLayoutParams(userLp);

        header.addView(avatar);
        header.addView(userTxt);
        card.addView(header);

        // Post Image
        final ImageView postImgView = new ImageView(this);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600
        );
        postImgView.setLayoutParams(imgLp);
        postImgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        postImgView.setBackgroundColor(Color.parseColor("#F0F0F0"));
        card.addView(postImgView);

        // Actions Row (Like / Comment / Web Share)
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(24, 16, 24, 8);

        final Button btnLike = new Button(this);
        btnLike.setText("❤ Like");
        btnLike.setTextSize(12sp);
        btnLike.setTextColor(Color.parseColor("#262626"));
        btnLike.setBackgroundColor(Color.parseColor("#FAFAFA"));

        Button btnComment = new Button(this);
        btnComment.setText("💬 Comment");
        btnComment.setTextSize(12sp);
        btnComment.setTextColor(Color.parseColor("#262626"));
        btnComment.setBackgroundColor(Color.parseColor("#FAFAFA"));

        Button btnWebOpen = new Button(this);
        btnWebOpen.setText("🌐 View on Web");
        btnWebOpen.setTextSize(12sp);
        btnWebOpen.setTextColor(Color.parseColor("#E1306C"));
        btnWebOpen.setBackgroundColor(Color.parseColor("#FAFAFA"));

        actions.addView(btnLike);
        actions.addView(btnComment);
        actions.addView(btnWebOpen);
        card.addView(actions);

        // Likes count
        final TextView likesTxt = new TextView(this);
        likesTxt.setText(post.likesCount + " likes");
        likesTxt.setTextSize(13sp);
        likesTxt.setTypeface(null, android.graphics.Typeface.BOLD);
        likesTxt.setTextColor(Color.parseColor("#262626"));
        likesTxt.setPadding(24, 0, 24, 4);
        card.addView(likesTxt);

        // Caption
        TextView captionTxt = new TextView(this);
        captionTxt.setText(post.username + " " + post.caption);
        captionTxt.setTextSize(13sp);
        captionTxt.setTextColor(Color.parseColor("#262626"));
        captionTxt.setPadding(24, 0, 24, 16);
        card.addView(captionTxt);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(Color.parseColor("#EFEFEF"));
        card.addView(divider);

        // Event listeners
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!post.isLiked) {
                    post.isLiked = true;
                    post.likesCount++;
                    btnLike.setTextColor(Color.RED);
                    btnLike.setText("❤ Liked");
                } else {
                    post.isLiked = false;
                    post.likesCount--;
                    btnLike.setTextColor(Color.parseColor("#262626"));
                    btnLike.setText("❤ Like");
                }
                likesTxt.setText(post.likesCount + " likes");
            }
        });

        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Comments feature available on Instagram Web", Toast.LENGTH_SHORT).show();
            }
        });

        btnWebOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInstagramWeb();
            }
        });

        postsContainer.addView(card);

        // Async Image Loading
        loadImageFromUrl(post.userAvatarUrl, avatar);
        loadImageFromUrl(post.postImageUrl, postImgView);
    }

    private void loadImageFromUrl(final String urlString, final ImageView imageView) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);

                    if (bitmap != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    // Retain fallback background
                }
            }
        }).start();
    }
}