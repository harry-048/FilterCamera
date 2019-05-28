package com.example.filtercamera;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class PoseImageAdapter extends RecyclerView.Adapter<PoseImageViewHolder> {

    ArrayList<Integer> poseImageNames;
    View view;
    Activity mContext;
    CardView poseCard;
    int cardSize=0;

    public PoseImageAdapter(int casdSize,ArrayList<Integer> poseImageNames, Activity mContext) {
        this.poseImageNames = poseImageNames;
        this.mContext = mContext;
        this.cardSize=casdSize;
    }

    @NonNull
    @Override
    public PoseImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.poses_images, parent, false);
        final PoseImageViewHolder viewHolder = new PoseImageViewHolder(view);

        poseCard = view.findViewById(R.id.poseCardView);
        Log.d("cardsize",cardSize+"");
        CardView.LayoutParams parms = new CardView.LayoutParams(cardSize,cardSize);
      //  poseCard.setLayoutParams(parms);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PoseImageViewHolder holder, int position) {

       // Picasso.with(mContext).load(poseImageNames.get(position)).into(holder.imageView);
        holder.imageView.setImageResource(poseImageNames.get(position));
       /* Glide.with(holder.itemView.getContext())
                .load(poseImageNames.get(position))
                .into(holder.imageView);*/
    }

    @Override
    public int getItemCount() {
        return poseImageNames.size();
    }
}
