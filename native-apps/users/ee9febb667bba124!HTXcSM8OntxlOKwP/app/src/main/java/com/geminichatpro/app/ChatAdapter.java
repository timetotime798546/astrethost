package com.geminichatpro.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ChatAdapter extends BaseAdapter {
    private Context context;
    private List<Message> messageList;
    private OnSpeakClickListener speakClickListener;

    public interface OnSpeakClickListener {
        void onSpeakClick(String text);
    }

    public ChatAdapter(Context context, List<Message> messageList, OnSpeakClickListener speakClickListener) {
        this.context = context;
        this.messageList = messageList;
        this.speakClickListener = speakClickListener;
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public Object getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
            holder = new ViewHolder();
            holder.layoutUser = (LinearLayout) convertView.findViewById(R.id.layout_user);
            holder.tvUserText = (TextView) convertView.findViewById(R.id.tv_user_text);
            holder.tvUserTime = (TextView) convertView.findViewById(R.id.tv_user_time);

            holder.layoutAi = (LinearLayout) convertView.findViewById(R.id.layout_ai);
            holder.tvAiText = (TextView) convertView.findViewById(R.id.tv_ai_text);
            holder.tvAiTime = (TextView) convertView.findViewById(R.id.tv_ai_time);
            holder.btnSpeak = (ImageButton) convertView.findViewById(R.id.btn_speak);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Message msg = messageList.get(position);

        if (msg.isUser()) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutAi.setVisibility(View.GONE);
            holder.tvUserText.setText(msg.getText());
            holder.tvUserTime.setText(msg.getTime());
        } else {
            holder.layoutUser.setVisibility(View.GONE);
            holder.layoutAi.setVisibility(View.VISIBLE);
            holder.tvAiText.setText(msg.getText());
            holder.tvAiTime.setText(msg.getTime());

            holder.btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (speakClickListener != null) {
                        speakClickListener.onSpeakClick(msg.getText());
                    }
                } 
            });
        }

        return convertView;
    }

    private static class ViewHolder {
        LinearLayout layoutUser;
        TextView tvUserText;
        TextView tvUserTime;

        LinearLayout layoutAi;
        TextView tvAiText;
        TextView tvAiTime;
        ImageButton btnSpeak;
    }
}