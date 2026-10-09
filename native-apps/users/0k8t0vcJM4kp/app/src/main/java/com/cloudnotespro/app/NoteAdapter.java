package com.cloudnotespro.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
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
        
        String desc = note.getDescription();
        if (desc.length() > 65) {
            desc = desc.substring(0, 62) + "...";
        }
        txtDesc.setText(desc);
        txtDate.setText(note.getDate());
        
        badgeType.setText(note.getType().toUpperCase());
        badgeStatus.setText(note.getStatus());

        // Gorgeous custom badge colors
        int typeColor = getTypeColor(note.getType());
        GradientDrawable typeShape = new GradientDrawable();
        typeShape.setShape(GradientDrawable.RECTANGLE);
        typeShape.setCornerRadius(16f);
        typeShape.setColor(typeColor);
        badgeType.setBackground(typeShape);

        int statusColor = note.getStatus().equalsIgnoreCase("Completed") ? 0xFF10B981 : 0xFFF59E0B;
        GradientDrawable statusShape = new GradientDrawable();
        statusShape.setShape(GradientDrawable.RECTANGLE);
        statusShape.setCornerRadius(16f);
        statusShape.setColor(statusColor);
        badgeStatus.setBackground(statusShape);

        // Dynamic category cover image
        if (note.getImageUrl() != null && !note.getImageUrl().isEmpty()) {
            ImageLoader.loadImage(note.getImageUrl(), imageThumbnail);
        } else {
            imageThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

    private int getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "work":
                return 0xFF6366F1; // Indigo Premium
            case "personal":
                return 0xFFEC4899; // Pink Premium
            case "ideas":
                return 0xFF8B5CF6; // Violet Premium
            case "journal":
                return 0xFF06B6D4; // Cyan Premium
            default:
                return 0xFF64748B; // Slate Premium
        }
    }

    private static final int R_id_note_image = 1001;
    private static final int R_id_note_title = 1002;
    private static final int R_id_note_desc = 1003;
    private static final int R_id_note_date = 1004;
    private static final int R_id_note_badge_type = 1005;
    private static final int R_id_note_badge_status = 1006;

    private View createCustomNoteView() {
        android.widget.RelativeLayout container = new android.widget.RelativeLayout(context);
        container.setPadding(24, 24, 24, 24);
        
        // Elite Rounded Card Outline Style
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.WHITE);
        gd.setCornerRadius(24f);
        gd.setStroke(1, 0xFFE2E8F0);
        container.setBackground(gd);

        android.widget.AbsListView.LayoutParams layoutParams = new android.widget.AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(layoutParams);

        // Left Cover Image View
        ImageView imageThumbnail = new ImageView(context);
        imageThumbnail.setId(R_id_note_image);
        imageThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Clip rounded corners on thumbnail
        GradientDrawable imageBg = new GradientDrawable();
        imageBg.setColor(0xFFF1F5F9);
        imageBg.setCornerRadius(16f);
        imageThumbnail.setBackground(imageBg);

        android.widget.RelativeLayout.LayoutParams imgParams = new android.widget.RelativeLayout.LayoutParams(180, 180);
        imgParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
        imgParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        imgParams.rightMargin = 24;
        imageThumbnail.setLayoutParams(imgParams);
        container.addView(imageThumbnail);

        // Content Area Right
        android.widget.LinearLayout infoLayout = new android.widget.LinearLayout(context);
        infoLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.RelativeLayout.LayoutParams infoParams = new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.addRule(android.widget.RelativeLayout.RIGHT_OF, R_id_note_image);
        infoParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        infoLayout.setLayoutParams(infoParams);

        // Elegant Premium Typography Title
        TextView titleText = new TextView(context);
        titleText.setId(R_id_note_title);
        titleText.setTextSize(17f);
        titleText.setTextColor(0xFF1E293B);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setSingleLine(true);
        infoLayout.addView(titleText);

        // Short Description
        TextView descText = new TextView(context);
        descText.setId(R_id_note_desc);
        descText.setTextSize(13.5f);
        descText.setTextColor(0xFF64748B);
        descText.setPadding(0, 4, 0, 8);
        descText.setMaxLines(2);
        infoLayout.addView(descText);

        // Row for metadata (Date, category and status badges)
        android.widget.LinearLayout badgeRow = new android.widget.LinearLayout(context);
        badgeRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView dateText = new TextView(context);
        dateText.setId(R_id_note_date);
        dateText.setTextSize(11f);
        dateText.setTextColor(0xFF94A3B8);
        android.widget.LinearLayout.LayoutParams dateParams = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        dateText.setLayoutParams(dateParams);
        badgeRow.addView(dateText);

        TextView typeText = new TextView(context);
        typeText.setId(R_id_note_badge_type);
        typeText.setTextSize(9.5f);
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
        statusText.setTextSize(9.5f);
        statusText.setTextColor(Color.WHITE);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setPadding(16, 6, 16, 6);
        badgeRow.addView(statusText);

        infoLayout.addView(badgeRow);
        container.addView(infoLayout);

        // Outer wrapper padding for smooth card spacing
        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(context);
        wrapper.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrapper.setPadding(24, 12, 24, 12);
        wrapper.addView(container);

        return wrapper;
    }
}