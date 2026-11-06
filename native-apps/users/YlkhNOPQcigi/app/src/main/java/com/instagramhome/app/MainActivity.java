package com.instagramhome.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout storiesContainer;
    private LinearLayout postsContainer;
    private LinearLayout exploreGridContainer;
    private LinearLayout profileContainer;
    
    private ScrollView feedScrollView;
    private ScrollView exploreScrollView;
    private ScrollView profileScrollView;
    
    private EditText etSearch;
    private Button btnNotifications;
    private Button btnDirectMessage;
    
    private Button navHome;
    private Button navSearch;
    private Button navAdd;
    private Button navReels;
    private Button navProfile;

    private List<PostModel> allPosts;
    private List<StoryModel> allStories;

    private static class StoryModel {
        String username;
        String emoji;
        boolean hasUnseen;

        StoryModel(String username, String emoji, boolean hasUnseen) {
            this.username = username;
            this.emoji = emoji;
            this.hasUnseen = hasUnseen;
        }
    }

    private static class PostModel {
        int id;
        String username;
        String userEmoji;
        String location;
        String imageEmoji;
        String imageBgColor;
        int likesCount;
        boolean isLiked;
        boolean isSaved;
        String caption;
        String timeAgo;
        List<String> comments;

        PostModel(int id, String username, String userEmoji, String location, 
                  String imageEmoji, String imageBgColor, int likesCount, 
                  String caption, String timeAgo) {
            this.id = id;
            this.username = username;
            this.userEmoji = userEmoji;
            this.location = location;
            this.imageEmoji = imageEmoji;
            this.imageBgColor = imageBgColor;
            this.likesCount = likesCount;
            this.isLiked = false;
            this.isSaved = false;
            this.caption = caption;
            this.timeAgo = timeAgo;
            this.comments = new ArrayList<String>();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initData();
        renderStories();
        renderPosts(allPosts);
        renderExploreGrid();
        renderProfileView();
        setupListeners();
    }

    private void initViews() {
        storiesContainer = (LinearLayout) findViewById(R.id.storiesContainer);
        postsContainer = (LinearLayout) findViewById(R.id.postsContainer);
        exploreGridContainer = (LinearLayout) findViewById(R.id.exploreGridContainer);
        profileContainer = (LinearLayout) findViewById(R.id.profileContainer);

        feedScrollView = (ScrollView) findViewById(R.id.feedScrollView);
        exploreScrollView = (ScrollView) findViewById(R.id.exploreScrollView);
        profileScrollView = (ScrollView) findViewById(R.id.profileScrollView);

        etSearch = (EditText) findViewById(R.id.etSearch);
        btnNotifications = (Button) findViewById(R.id.btnNotifications);
        btnDirectMessage = (Button) findViewById(R.id.btnDirectMessage);

        navHome = (Button) findViewById(R.id.navHome);
        navSearch = (Button) findViewById(R.id.navSearch);
        navAdd = (Button) findViewById(R.id.navAdd);
        navReels = (Button) findViewById(R.id.navReels);
        navProfile = (Button) findViewById(R.id.navProfile);
    }

    private void initData() {
        allStories = new ArrayList<StoryModel>();
        allStories.add(new StoryModel("Your Story", "➕", false));
        allStories.add(new StoryModel("alex_dev", "👨‍💻", true));
        allStories.add(new StoryModel("travel_bug", "✈️", true));
        allStories.add(new StoryModel("photo_studio", "📷", true));
        allStories.add(new StoryModel("tech_daily", "📱", true));
        allStories.add(new StoryModel("fitness_pro", "🏋️‍♂️", false));
        allStories.add(new StoryModel("nature_world", "🌿", false));

        allPosts = new ArrayList<PostModel>();

        PostModel p1 = new PostModel(1, "alex_dev", "👨‍💻", "San Francisco, CA",
                "💻🚀✨", "#3B82F6", 1240, 
                "Building native Android UI components with full touch responsiveness! #androiddev #coding #java", 
                "2 HOURS AGO");
        p1.comments.add("sarah_design: Looking clean!");
        p1.comments.add("mike_code: Great architecture!");
        allPosts.add(p1);

        PostModel p2 = new PostModel(2, "travel_bug", "✈️", "Kyoto, Japan",
                "⛩️🌸🏯", "#EF4444", 3890, 
                "Sunset walks around historic temples in Kyoto. Japan is truly magical in autumn 🌸 #travel #japan", 
                "4 HOURS AGO");
        p2.comments.add("wanderer99: Added to my travel list!");
        allPosts.add(p2);

        PostModel p3 = new PostModel(3, "photo_studio", "📷", "Studio A, New York",
                "📸💡🎨", "#8B5CF6", 845, 
                "New minimal lighting setup for portraits. What do you think of this contrast? #photography #studio", 
                "6 HOURS AGO");
        allPosts.add(p3);

        PostModel p4 = new PostModel(4, "tech_daily", "📱", "Silicon Valley",
                "⚡🔋📲", "#10B981", 5120, 
                "Next-gen processors push the boundary of mobile AI performance. #tech #gadgets #future", 
                "12 HOURS AGO");
        allPosts.add(p4);
    }

    private void renderStories() {
        storiesContainer.removeAllViews();
        for (int i = 0; i < allStories.size(); i++) {
            final StoryModel story = allStories.get(i);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL);
            item.setPadding(12, 0, 12, 0);

            // Ring Container
            FrameLayout ring = new FrameLayout(this);
            LinearLayout.LayoutParams ringParams = new LinearLayout.LayoutParams(140, 140);
            ring.setLayoutParams(ringParams);
            
            if (story.hasUnseen) {
                ring.setBackgroundColor(Color.parseColor("#E1306C")); // Instagram pink ring border
            } else {
                ring.setBackgroundColor(Color.parseColor("#CCCCCC"));
            }
            ring.setPadding(4, 4, 4, 4);

            TextView avatar = new TextView(this);
            avatar.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            avatar.setText(story.emoji);
            avatar.setTextSize(26sp());
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackgroundColor(Color.WHITE);
            ring.addView(avatar);

            TextView tvName = new TextView(this);
            tvName.setText(story.username);
            tvName.setTextSize(11sp());
            tvName.setTextColor(Color.parseColor("#262626"));
            tvName.setGravity(Gravity.CENTER);
            tvName.setPadding(0, 8, 0, 0);

            item.addView(ring);
            item.addView(tvName);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    story.hasUnseen = false;
                    renderStories();
                    Toast.makeText(MainActivity.this, "Viewing " + story.username + "'s story", Toast.LENGTH_SHORT).show();
                }
            });

            storiesContainer.addView(item);
        }
    }

    private void renderPosts(List<PostModel> postsToDisplay) {
        postsContainer.removeAllViews();

        if (postsToDisplay.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No posts found matching your search.");
            emptyText.setTextSize(14sp());
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 60, 0, 60);
            postsContainer.addView(emptyText);
            return;
        }

        for (int i = 0; i < postsToDisplay.size(); i++) {
            final PostModel post = postsToDisplay.get(i);

            LinearLayout postCard = new LinearLayout(this);
            postCard.setOrientation(LinearLayout.VERTICAL);
            postCard.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            postCard.setLayoutParams(cardParams);

            // 1. Post Header
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(16, 12, 16, 12);

            TextView userAvatar = new TextView(this);
            userAvatar.setText(post.userEmoji);
            userAvatar.setTextSize(20sp());
            userAvatar.setPadding(0, 0, 12, 0);

            LinearLayout userMeta = new LinearLayout(this);
            userMeta.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            userMeta.setLayoutParams(metaParams);

            TextView tvUsername = new TextView(this);
            tvUsername.setText(post.username);
            tvUsername.setTypeface(null, Typeface.BOLD);
            tvUsername.setTextSize(14sp());
            tvUsername.setTextColor(Color.BLACK);

            TextView tvLocation = new TextView(this);
            tvLocation.setText(post.location);
            tvLocation.setTextSize(11sp());
            tvLocation.setTextColor(Color.GRAY);

            userMeta.addView(tvUsername);
            userMeta.addView(tvLocation);

            TextView btnMore = new TextView(this);
            btnMore.setText("•••");
            btnMore.setTextSize(14sp());
            btnMore.setTextColor(Color.BLACK);
            btnMore.setPadding(8, 8, 8, 8);

            header.addView(userAvatar);
            header.addView(userMeta);
            header.addView(btnMore);
            postCard.addView(header);

            // 2. Post Media Canvas
            FrameLayout mediaCanvas = new FrameLayout(this);
            mediaCanvas.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 650));
            mediaCanvas.setBackgroundColor(Color.parseColor(post.imageBgColor));

            TextView mediaEmoji = new TextView(this);
            mediaEmoji.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mediaEmoji.setText(post.imageEmoji);
            mediaEmoji.setTextSize(64sp());
            mediaEmoji.setGravity(Gravity.CENTER);
            mediaEmoji.setTextColor(Color.WHITE);
            mediaCanvas.addView(mediaEmoji);

            // Double tap to like feature
            mediaCanvas.setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;
                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 400) {
                        if (!post.isLiked) {
                            post.isLiked = true;
                            post.likesCount++;
                            renderPosts(allPosts);
                        }
                        Toast.makeText(MainActivity.this, "❤️ Liked!", Toast.LENGTH_SHORT).show();
                    }
                    lastClickTime = clickTime;
                }
            });

            postCard.addView(mediaCanvas);

            // 3. Action Buttons Row
            LinearLayout actionsRow = new LinearLayout(this);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            actionsRow.setGravity(Gravity.CENTER_VERTICAL);
            actionsRow.setPadding(12, 10, 12, 6);

            final Button btnLike = new Button(this);
            btnLike.setText(post.isLiked ? "❤️" : "🤍");
            btnLike.setTextSize(18sp());
            btnLike.setBackgroundColor(Color.TRANSPARENT);
            btnLike.setLayoutParams(new LinearLayout.LayoutParams(110, 100));

            btnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    post.isLiked = !post.isLiked;
                    if (post.isLiked) {
                        post.likesCount++;
                    } else {
                        post.likesCount--;
                    }
                    renderPosts(allPosts);
                }
            });

            final Button btnComment = new Button(this);
            btnComment.setText("💬");
            btnComment.setTextSize(18sp());
            btnComment.setBackgroundColor(Color.TRANSPARENT);
            btnComment.setLayoutParams(new LinearLayout.LayoutParams(110, 100));

            btnComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCommentDialog(post);
                }
            });

            Button btnShare = new Button(this);
            btnShare.setText("✈️");
            btnShare.setTextSize(18sp());
            btnShare.setBackgroundColor(Color.TRANSPARENT);
            btnShare.setLayoutParams(new LinearLayout.LayoutParams(110, 100));
            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Share link copied for " + post.username + "'s post", Toast.LENGTH_SHORT).show();
                }
            });

            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));

            final Button btnBookmark = new Button(this);
            btnBookmark.setText(post.isSaved ? "🔖" : "🏷️");
            btnBookmark.setTextSize(18sp());
            btnBookmark.setBackgroundColor(Color.TRANSPARENT);
            btnBookmark.setLayoutParams(new LinearLayout.LayoutParams(110, 100));

            btnBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    post.isSaved = !post.isSaved;
                    renderPosts(allPosts);
                    String msg = post.isSaved ? "Post saved to collection" : "Post removed from collection";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });

            actionsRow.addView(btnLike);
            actionsRow.addView(btnComment);
            actionsRow.addView(btnShare);
            actionsRow.addView(spacer);
            actionsRow.addView(btnBookmark);
            postCard.addView(actionsRow);

            // 4. Likes Counter
            TextView tvLikes = new TextView(this);
            tvLikes.setText(post.likesCount + " likes");
            tvLikes.setTypeface(null, Typeface.BOLD);
            tvLikes.setTextSize(13sp());
            tvLikes.setTextColor(Color.BLACK);
            tvLikes.setPadding(16, 0, 16, 4);
            postCard.addView(tvLikes);

            // 5. Caption & Username
            LinearLayout captionLayout = new LinearLayout(this);
            captionLayout.setOrientation(LinearLayout.HORIZONTAL);
            captionLayout.setPadding(16, 0, 16, 6);

            TextView tvCaption = new TextView(this);
            tvCaption.setText(post.username + " " + post.caption);
            tvCaption.setTextSize(13sp());
            tvCaption.setTextColor(Color.BLACK);
            captionLayout.addView(tvCaption);
            postCard.addView(captionLayout);

            // 6. Comments Summary & List
            if (!post.comments.isEmpty()) {
                TextView tvViewComments = new TextView(this);
                tvViewComments.setText("View all " + post.comments.size() + " comments");
                tvViewComments.setTextSize(12sp());
                tvViewComments.setTextColor(Color.GRAY);
                tvViewComments.setPadding(16, 2, 16, 4);

                tvViewComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCommentDialog(post);
                    }
                });

                postCard.addView(tvViewComments);

                // Show latest comment snippet
                TextView tvLatestComment = new TextView(this);
                tvLatestComment.setText(post.comments.get(post.comments.size() - 1));
                tvLatestComment.setTextSize(12sp());
                tvLatestComment.setTextColor(Color.parseColor("#333333"));
                tvLatestComment.setPadding(16, 0, 16, 4);
                postCard.addView(tvLatestComment);
            }

            // 7. Time Stamp
            TextView tvTime = new TextView(this);
            tvTime.setText(post.timeAgo);
            tvTime.setTextSize(10sp());
            tvTime.setTextColor(Color.GRAY);
            tvTime.setPadding(16, 2, 16, 12);
            postCard.addView(tvTime);

            postsContainer.addView(postCard);
        }
    }

    private void showCommentDialog(final PostModel post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Comments on " + post.username + "'s post");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        // List existing comments
        for (String c : post.comments) {
            TextView commentItem = new TextView(this);
            commentItem.setText("• " + c);
            commentItem.setTextSize(13sp());
            commentItem.setTextColor(Color.BLACK);
            commentItem.setPadding(0, 6, 0, 6);
            layout.addView(commentItem);
        }

        final EditText etNewComment = new EditText(this);
        etNewComment.setHint("Add a comment as instagram_user...");
        etNewComment.setTextSize(13sp());
        layout.addView(etNewComment);

        builder.setView(layout);
        builder.setPositiveButton("Post", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = etNewComment.getText().toString().trim();
                if (!input.isEmpty()) {
                    post.comments.add("you: " + input);
                    renderPosts(allPosts);
                    Toast.makeText(MainActivity.this, "Comment added!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void renderExploreGrid() {
        exploreGridContainer.removeAllViews();

        TextView header = new TextView(this);
        header.setText("Explore Trending Content");
        header.setTextSize(18sp());
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(Color.BLACK);
        header.setPadding(12, 16, 12, 16);
        exploreGridContainer.addView(header);

        String[] exploreEmojis = {"🏝️", "🏎️", "🎨", "🍲", "📱", "🌌", "👟", "🎧", "🏔️"};
        String[] exploreColors = {"#F59E0B", "#EF4444", "#10B981", "#6366F1", "#8B5CF6", "#EC4899", "#3B82F6", "#14B8A6", "#84CC16"};

        LinearLayout row = null;
        for (int i = 0; i < exploreEmojis.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 260));
                exploreGridContainer.addView(row);
            }

            FrameLayout tile = new FrameLayout(this);
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            tileParams.setMargins(4, 4, 4, 4);
            tile.setLayoutParams(tileParams);
            tile.setBackgroundColor(Color.parseColor(exploreColors[i]));

            TextView emojiView = new TextView(this);
            emojiView.setText(exploreEmojis[i]);
            emojiView.setTextSize(36sp());
            emojiView.setGravity(Gravity.CENTER);
            tile.addView(emojiView);

            final int index = i;
            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Selected explore category #" + (index + 1), Toast.LENGTH_SHORT).show();
                }
            });

            if (row != null) {
                row.addView(tile);
            }
        }
    }

    private void renderProfileView() {
        profileContainer.removeAllViews();

        // Profile Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 16, 0, 16);

        TextView avatar = new TextView(this);
        avatar.setText("👤");
        avatar.setTextSize(48sp());
        avatar.setPadding(16, 16, 24, 16);
        avatar.setBackgroundColor(Color.parseColor("#F3F4F6"));

        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        statsLayout.setGravity(Gravity.CENTER);

        statsLayout.addView(createStatColumn("12", "Posts"));
        statsLayout.addView(createStatColumn("1.2k", "Followers"));
        statsLayout.addView(createStatColumn("450", "Following"));

        header.addView(avatar);
        header.addView(statsLayout);
        profileContainer.addView(header);

        // Bio Section
        TextView tvName = new TextView(this);
        tvName.setText("Instagram Developer");
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextSize(14sp());
        tvName.setTextColor(Color.BLACK);

        TextView tvBio = new TextView(this);
        tvBio.setText("🚀 Android Native Application Developer\n📸 Photography & Mobile UI Enthusiast\n📍 San Francisco, CA");
        tvBio.setTextSize(13sp());
        tvBio.setTextColor(Color.parseColor("#333333"));
        tvBio.setPadding(0, 4, 0, 16);

        profileContainer.addView(tvName);
        profileContainer.addView(tvBio);

        // Edit Profile Button
        Button btnEditProfile = new Button(this);
        btnEditProfile.setText("Edit Profile");
        btnEditProfile.setTextSize(13sp());
        btnEditProfile.setBackgroundColor(Color.parseColor("#EFEFEF"));
        btnEditProfile.setTextColor(Color.BLACK);
        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Edit Profile options", Toast.LENGTH_SHORT).show();
            }
        });
        profileContainer.addView(btnEditProfile);
    }

    private LinearLayout createStatColumn(String count, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(16, 0, 16, 0);

        TextView tvCount = new TextView(this);
        tvCount.setText(count);
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setTextSize(16sp());
        tvCount.setTextColor(Color.BLACK);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12sp());
        tvLabel.setTextColor(Color.GRAY);

        col.addView(tvCount);
        col.addView(tvLabel);
        return col;
    }

    private void setupListeners() {
        // Search Filter Logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    renderPosts(allPosts);
                } else {
                    List<PostModel> filtered = new ArrayList<PostModel>();
                    for (PostModel p : allPosts) {
                        if (p.username.toLowerCase().contains(query) ||
                            p.caption.toLowerCase().contains(query) ||
                            p.location.toLowerCase().contains(query)) {
                            filtered.add(p);
                        }
                    }
                    renderPosts(filtered);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Top bar buttons
        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Notifications: No new activity", Toast.LENGTH_SHORT).show();
            }
        });

        btnDirectMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Direct Messages opening...", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom Tab switching
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(feedScrollView);
            }
        });

        navSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(exploreScrollView);
            }
        });

        navAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Create new post screen...", Toast.LENGTH_SHORT).show();
            }
        });

        navReels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Reels feed loaded!", Toast.LENGTH_SHORT).show();
            }
        });

        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(profileScrollView);
            }
        });
    }

    private void showTab(View targetView) {
        feedScrollView.setVisibility(View.GONE);
        exploreScrollView.setVisibility(View.GONE);
        profileScrollView.setVisibility(View.GONE);

        targetView.setVisibility(View.VISIBLE);
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private float sp2px(float sp) {
        return spToPx(sp);
    }

    private float 14sp() { return 14; }
    private float 11sp() { return 11; }
    private float 12sp() { return 12; }
    private float 13sp() { return 13; }
    private float 10sp() { return 10; }
    private float 16sp() { return 16; }
    private float 18sp() { return 18; }
    private float 20sp() { return 20; }
    private float 24sp() { return 24; }
    private float 26sp() { return 26; }
    private float 36sp() { return 36; }
    private float 48sp() { return 48; }
    private float 64sp() { return 64; }
}