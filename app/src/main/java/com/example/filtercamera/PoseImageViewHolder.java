package com.example.filtercamera;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PoseImageViewHolder extends RecyclerView.ViewHolder {
    ImageView imageView;
    public PoseImageViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.poseImage);
    }
}
