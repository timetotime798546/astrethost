package com.cloudnotespro.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class NoteAdapter extends BaseAdapter {
    private final Context context;
    private final List<Note> notes;

    public NoteAdapter(Context context, List<Note> notes) {
        this.context = context;
        this.notes = notes;
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public Object getItem(int position) {
        return notes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
            // Replace standard single text with custom note list layout programmatically to ensure dynamic styling
            convertView = createCustomNoteView();
        }

        Note note = notes.get(position);

        ImageView imageThumbnail = (ImageView) convertView.findViewById(R_id_note_image);
        TextView txtTitle = (TextView) convertView.findViewById(R_id_note_title);
        TextView txtDesc = (TextView) convertView.findViewById(R_id_note_desc);
        TextView txtDate = (TextView) convertView.findViewById(R_id_note_date);
        TextView badgeType = (TextView) convertView.findViewById(R_id_note_badge_type);
        TextView badgeStatus = (TextView) convertView.findViewById(R_id_note_badge_status);

        txtTitle.setText(note.getTitle());
        
        // Truncate description for tidy display
        String desc = note.getDescription();
        if (desc.length() > 60) {
            desc = desc.substring(0, 57) + "...";
        }
        txtDesc.setText(desc);
        txtDate.setText(note.getDate());
        
        badgeType.setText(note.getType().toUpperCase());
        badgeStatus.setText(note.getStatus());

        // Dynamic badges styling
        int typeColor = getTypeColor(note.getType());
        GradientDrawable typeShape = new GradientDrawable();
        typeShape.setShape(GradientDrawable.RECTANGLE);
        typeShape.setCornerRadius(12f);
        typeShape.setColor(typeColor);
        badgeType.setBackground(typeShape);

        int statusColor = note.getStatus().equalsIgnoreCase("Completed") ? 0xFF2E7D32 : 0xFFD84315;
        GradientDrawable statusShape = new GradientDrawable();
        statusShape.setShape(GradientDrawable.RECTANGLE);
        statusShape.setCornerRadius(12f);
        statusShape.setColor(statusColor);
        badgeStatus.setBackground(statusShape);

        // Load dynamic category photo
        ImageLoader.loadImage(note.getImageUrl(), imageThumbnail);

        return convertView;
    }

    private int getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return 0xFF1976D2; // Blue
            case "personal":
                return 0xFF7B1FA2; // Purple
            case "ideas":
                return 0xFFFBC02D; // Yellow/Amber
            case "journal":
                return 0xFF388E3C; // Green
            default:
                return 0xFF616161; // Grey
        }
    }

    // Custom programmatically defined IDs to work around resource build quirks and maintain native standalone build
    private static final int R_id_note_image = 1001;
    private static final int R_id_note_title = 1002;
    private static final int R_id_note_desc = 1003;
    private static final int R_id_note_date = 1004;
    private static final int R_id_note_badge_type = 1005;
    private static final int R_id_note_badge_status = 1006;

    private View createCustomNoteView() {
        android.widget.RelativeLayout container = new android.widget.RelativeLayout(context);
        container.setPadding(32, 24, 32, 24);
        
        // Rounded card background
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.WHITE);
        gd.setCornerRadius(16f);
        gd.setStroke(2, 0xFFE0E0E0);
        container.setBackground(gd);

        // Simple margin between card rows
        android.widget.AbsListView.LayoutParams layoutParams = new android.widget.AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(layoutParams);

        // Note Thumbnail Image aligned left
        ImageView imageThumbnail = new ImageView(context);
        imageThumbnail.setId(R_id_note_image);
        imageThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(160, 160);
        imgParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
        imgParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        imgParams.rightMargin = 24;
        imageThumbnail.setLayoutParams(imgParams);
        container.addView(imageThumbnail);

        // Right Content Container
        android.widget.LinearLayout infoLayout = new android.widget.LinearLayout(context);
        infoLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.RelativeLayout.LayoutParams infoParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.addRule(android.widget.RelativeLayout.RIGHT_OF, R_id_note_image);
        infoParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        infoLayout.setLayoutParams(infoParams);

        // Note Title
        TextView titleText = new TextView(context);
        titleText.setId(R_id_note_title);
        titleText.setTextSize(18f);
        titleText.setTextColor(0xFF212121);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        infoLayout.addView(titleText);

        // Note Short Description
        TextView descText = new TextView(context);
        descText.setId(R_id_note_desc);
        descText.setTextSize(14f);
        descText.setTextColor(0xFF666666);
        descText.setPadding(0, 6, 0, 8);
        infoLayout.addView(descText);

        // Row containing Date, Type and Status Badges
        android.widget.LinearLayout badgeRow = new android.widget.LinearLayout(context);
        badgeRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        badgeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView dateText = new TextView(context);
        dateText.setId(R_id_note_date);
        dateText.setTextSize(12f);
        dateText.setTextColor(0xFF888888);
        android.widget.LinearLayout.LayoutParams dateParams = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        dateText.setLayoutParams(dateParams);
        badgeRow.addView(dateText);

        TextView typeText = new TextView(context);
        typeText.setId(R_id_note_badge_type);
        typeText.setTextSize(10f);
        typeText.setTextColor(Color.WHITE);
        typeText.setTypeface(null, android.graphics.Typeface.BOLD);
        typeText.setPadding(16, 6, 16, 6);
        android.widget.LinearLayout.LayoutParams typeParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        typeParams.rightMargin = 12;
        typeText.setLayoutParams(typeParams);
        badgeRow.addView(typeText);

        TextView statusText = new TextView(context);
        statusText.setId(R_id_note_badge_status);
        statusText.setTextSize(10f);
        statusText.setTextColor(Color.WHITE);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setPadding(16, 6, 16, 6);
        badgeRow.addView(statusText);

        infoLayout.addView(badgeRow);
        container.addView(infoLayout);

        // Extra outer container to act as list item wrapper with margin space
        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(context);
        wrapper.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrapper.setPadding(16, 8, 16, 8);
        wrapper.addView(container);

        return wrapper;
    }
}