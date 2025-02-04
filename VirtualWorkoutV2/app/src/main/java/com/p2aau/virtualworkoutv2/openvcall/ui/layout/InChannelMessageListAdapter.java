package com.p2aau.virtualworkoutv2.openvcall.ui.layout;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import com.p2aau.virtualworkoutv2.R;
import com.p2aau.virtualworkoutv2.openvcall.model.Message;

public class InChannelMessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Message> mMsgList;

    private final LayoutInflater mInflater;

    public InChannelMessageListAdapter(Activity activity, ArrayList<Message> list) {
        mInflater = activity.getLayoutInflater();
        mMsgList = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.agora_in_channel_message, parent, false);
        return new MessageHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message msg = mMsgList.get(position);

        MessageHolder myHolder = (MessageHolder) holder;
        String sender = msg.getSender().name;
        if (TextUtils.isEmpty(sender)) {
            myHolder.itemView.setBackgroundResource(R.drawable.agora_rounded_bg_blue);
        } else {
            myHolder.itemView.setBackgroundResource(R.drawable.agora_rounded_bg);
        }
        myHolder.mMsgContent.setText(msg.getContent());
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

    @Override
    public long getItemId(int position) {
        return mMsgList.get(position).hashCode();
    }

    public class MessageHolder extends RecyclerView.ViewHolder {
        TextView mMsgContent;

        MessageHolder(View v) {
            super(v);
            mMsgContent = (TextView) v.findViewById(R.id.msg_content);
        }
    }
}
