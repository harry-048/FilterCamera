package com.example.filtercamera;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ThumbnailsViewHolder extends RecyclerView.ViewHolder {
    ImageView thumbnail;
    TextView thumnailNameTextView;
    public ThumbnailsViewHolder(@NonNull View itemView) {
        super(itemView);
        thumbnail = itemView.findViewById(R.id.thumbnail);
        thumnailNameTextView = itemView.findViewById(R.id.thumnailName);
    }
}
