package com.facebookhome.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private LinearLayout feedContainer;
    private LinearLayout storiesContainer;
    private List<PostItem> postsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        feedContainer = findViewById(R.id.feedContainer);
        storiesContainer = findViewById(R.id.storiesContainer);

        postsList = new ArrayList<>();
        initSampleData();

        setupHeaderButtons();
        renderStories();
        renderFeed();
    }

    private void setupHeaderButtons() {
        Button btnOpenWeb = findViewById(R.id.btnOpenWeb);
        btnOpenWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebFacebook();
            }
        });

        TextView btnCreatePostTrigger = findViewById(R.id.btnCreatePostTrigger);
        btnCreatePostTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePostDialog();
            }
        });

        TextView tabMenu = findViewById(R.id.tabMenu);
        tabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebFacebook();
            }
        });

        Button btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Search active. Typing query...", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnMessenger = findViewById(R.id.btnMessenger);
        btnMessenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Opening Messenger Messages...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openWebFacebook() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Unable to launch system browser", Toast.LENGTH_SHORT).show();
        }
    }

    private void initSampleData() {
        PostItem p1 = new PostItem(
                "1",
                "Tech Insider",
                "20 mins ago",
                "Artificial Intelligence is reshaping how mobile applications deliver real-time user experiences natively across devices!",
                Color.parseColor("#1877F2"),
                142
        );
        p1.addComment(new CommentItem("David Miller", "Awesome update!", "10m ago"));
        p1.addComment(new CommentItem("Sarah Connor", "Native Android code works super fast.", "5m ago"));

        PostItem p2 = new PostItem(
                "2",
                "Alex Rivera",
                "1 hour ago",
                "Just completed our annual tech conference in San Francisco! Thanks to everyone who joined us in person and online.",
                Color.parseColor("#42B72A"),
                89
        );
        p2.addComment(new CommentItem("Emma Watson", "Great presentation Alex!", "40m ago"));

        PostItem p3 = new PostItem(
                "3",
                "Global Travel & News",
                "3 hours ago",
                "Top 10 breathtaking destinations to visit this autumn season. What is your favorite holiday spot?",
                Color.parseColor("#F7B928"),
                310
        );

        postsList.add(p1);
        postsList.add(p2);
        postsList.add(p3);
    }

    private void renderStories() {
        storiesContainer.removeAllViews();

        String[] names = {"Create Story", "Sarah J.", "Tech Feed", "Alex R.", "Global News"};
        int[] colors = {Color.parseColor("#42B72A"), Color.parseColor("#1877F2"), Color.parseColor("#E7F3FF"), Color.parseColor("#F3425F"), Color.parseColor("#F7B928")};

        for (int i = 0; i < names.length; i++) {
            LinearLayout storyCard = new LinearLayout(this);
            storyCard.setOrientation(LinearLayout.VERTICAL);
            storyCard.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            storyCard.setPadding(8, 8, 8, 8);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(180, 260);
            lp.setMargins(6, 0, 6, 0);
            storyCard.setLayoutParams(lp);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(16);
            shape.setColor(colors[i]);
            storyCard.setBackground(shape);

            TextView txtName = new TextView(this);
            txtName.setText(names[i]);
            txtName.setTextColor(Color.WHITE);
            txtName.setTextSize(11);
            txtName.setTypeface(null, Typeface.BOLD);
            txtName.setGravity(Gravity.CENTER);

            storyCard.addView(txtName);

            final String storyOwner = names[i];
            storyCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Viewing " + storyOwner + "'s story", Toast.LENGTH_SHORT).show();
                }
            });

            storiesContainer.addView(storyCard);
        }
    }

    private void renderFeed() {
        feedContainer.removeAllViews();

        for (int i = 0; i < postsList.size(); i++) {
            final PostItem post = postsList.get(i);
            feedContainer.addView(createPostCard(post));
        }
    }

    private View createPostCard(final PostItem post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardLp);
        card.setPadding(16, 16, 16, 16);

        // Header: Author Avatar & Name
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        avatar.setText(post.getAuthorName().substring(0, 1));
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(16);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(post.getAvatarColor());
        avatar.setBackground(avatarBg);

        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(80, 80);
        avatarLp.setMargins(0, 0, 12, 0);
        avatar.setLayoutParams(avatarLp);

        LinearLayout authorDetails = new LinearLayout(this);
        authorDetails.setOrientation(LinearLayout.VERTICAL);

        TextView authorName = new TextView(this);
        authorName.setText(post.getAuthorName());
        authorName.setTextColor(Color.parseColor("#050505"));
        authorName.setTextSize(15);
        authorName.setTypeface(null, Typeface.BOLD);

        TextView timeStamp = new TextView(this);
        timeStamp.setText(post.getTimestamp() + " • 🌐");
        timeStamp.setTextColor(Color.parseColor("#65676B"));
        timeStamp.setTextSize(12);

        authorDetails.addView(authorName);
        authorDetails.addView(timeStamp);

        header.addView(avatar);
        header.addView(authorDetails);

        // Content Text
        TextView contentText = new TextView(this);
        contentText.setText(post.getContent());
        contentText.setTextColor(Color.parseColor("#050505"));
        contentText.setTextSize(14);
        contentText.setPadding(0, 16, 0, 16);

        // Reactions Counter Row
        LinearLayout reactionStats = new LinearLayout(this);
        reactionStats.setOrientation(LinearLayout.HORIZONTAL);
        reactionStats.setPadding(0, 8, 0, 8);

        final TextView txtLikes = new TextView(this);
        txtLikes.setText("👍 " + post.getLikeCount() + " Likes");
        txtLikes.setTextColor(Color.parseColor("#65676B"));
        txtLikes.setTextSize(12);

        TextView txtCommentsCount = new TextView(this);
        txtCommentsCount.setText(post.getComments().size() + " Comments");
        txtCommentsCount.setTextColor(Color.parseColor("#65676B"));
        txtCommentsCount.setTextSize(12);
        txtCommentsCount.setGravity(Gravity.END);

        LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        txtLikes.setLayoutParams(statLp);
        txtCommentsCount.setLayoutParams(statLp);

        reactionStats.addView(txtLikes);
        reactionStats.addView(txtCommentsCount);

        // Divider Line
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#CED0D4"));

        // Action Buttons Row (Like, Comment, Share)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 8, 0, 0);

        final Button btnLike = new Button(this);
        btnLike.setText(post.isLiked() ? "👍 Liked" : "👍 Like");
        btnLike.setTextColor(post.isLiked() ? Color.parseColor("#1877F2") : Color.parseColor("#65676B"));
        btnLike.setBackgroundColor(Color.TRANSPARENT);

        Button btnComment = new Button(this);
        btnComment.setText("💬 Comment");
        btnComment.setTextColor(Color.parseColor("#65676B"));
        btnComment.setBackgroundColor(Color.TRANSPARENT);

        Button btnShare = new Button(this);
        btnShare.setText("↗️ Share");
        btnShare.setTextColor(Color.parseColor("#65676B"));
        btnShare.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnLike.setLayoutParams(btnLp);
        btnComment.setLayoutParams(btnLp);
        btnShare.setLayoutParams(btnLp);

        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post.toggleLike();
                btnLike.setText(post.isLiked() ? "👍 Liked" : "👍 Like");
                btnLike.setTextColor(post.isLiked() ? Color.parseColor("#1877F2") : Color.parseColor("#65676B"));
                txtLikes.setText("👍 " + post.getLikeCount() + " Likes");
            }
        });

        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentDialog(post);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, post.getAuthorName() + ": " + post.getContent());
                startActivity(Intent.createChooser(shareIntent, "Share Facebook post via"));
            }
        });

        actionRow.addView(btnLike);
        actionRow.addView(btnComment);
        actionRow.addView(btnShare);

        // Assemble Card
        card.addView(header);
        card.addView(contentText);
        card.addView(reactionStats);
        card.addView(divider);
        card.addView(actionRow);

        return card;
    }

    private void showCreatePostDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Create Post");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#050505"));

        final EditText input = new EditText(this);
        input.setHint("What's on your mind?");
        input.setHeight(200);
        input.setGravity(Gravity.TOP);
        input.setBackgroundColor(Color.parseColor("#F0F2F5"));
        input.setPadding(16, 16, 16, 16);

        Button btnSubmit = new Button(this);
        btnSubmit.setText("Post");
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setBackgroundColor(Color.parseColor("#1877F2"));

        LinearLayout.LayoutParams marginLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        marginLp.setMargins(0, 16, 0, 16);

        layout.addView(title);
        layout.addView(input, marginLp);
        layout.addView(btnSubmit);

        dialog.setContentView(layout);
        dialog.show();

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = input.getText().toString().trim();
                if (text.length() > 0) {
                    PostItem newPost = new PostItem(
                            String.valueOf(System.currentTimeMillis()),
                            "You",
                            "Just now",
                            text,
                            Color.parseColor("#1877F2"),
                            0
                    );
                    postsList.add(0, newPost);
                    renderFeed();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Post published!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please write something first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCommentDialog(final PostItem post) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);
        mainLayout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Comments");
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);

        ScrollView commentScroll = new ScrollView(this);
        final LinearLayout commentContainer = new LinearLayout(this);
        commentContainer.setOrientation(LinearLayout.VERTICAL);
        commentScroll.addView(commentContainer);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300
        );
        mainLayout.addView(title);
        mainLayout.addView(commentScroll, scrollLp);

        renderCommentList(post, commentContainer);

        // Add comment box
        final EditText inputComment = new EditText(this);
        inputComment.setHint("Write a comment...");
        inputComment.setBackgroundColor(Color.parseColor("#F0F2F5"));
        inputComment.setPadding(16, 16, 16, 16);

        Button btnSend = new Button(this);
        btnSend.setText("Send");
        btnSend.setTextColor(Color.WHITE);
        btnSend.setBackgroundColor(Color.parseColor("#1877F2"));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 16, 0, 0);

        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        inputRow.addView(inputComment, inputLp);
        inputRow.addView(btnSend);

        mainLayout.addView(inputRow);

        dialog.setContentView(mainLayout);
        dialog.show();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commentText = inputComment.getText().toString().trim();
                if (commentText.length() > 0) {
                    post.addComment(new CommentItem("You", commentText, "Just now"));
                    inputComment.setText("");
                    renderCommentList(post, commentContainer);
                    renderFeed();
                    Toast.makeText(MainActivity.this, "Comment added!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void renderCommentList(PostItem post, LinearLayout container) {
        container.removeAllViews();
        List<CommentItem> list = post.getComments();
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No comments yet. Be the first to comment!");
            empty.setTextColor(Color.parseColor("#65676B"));
            empty.setPadding(0, 16, 0, 16);
            container.addView(empty);
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            CommentItem item = list.get(i);
            LinearLayout commentBox = new LinearLayout(this);
            commentBox.setOrientation(LinearLayout.VERTICAL);
            commentBox.setBackgroundColor(Color.parseColor("#F0F2F5"));
            commentBox.setPadding(12, 12, 12, 12);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 8);
            commentBox.setLayoutParams(lp);

            TextView author = new TextView(this);
            author.setText(item.getAuthor());
            author.setTypeface(null, Typeface.BOLD);
            author.setTextSize(13);

            TextView text = new TextView(this);
            text.setText(item.getText());
            text.setTextSize(13);

            commentBox.addView(author);
            commentBox.addView(text);

            container.addView(commentBox);
        }
    }
}