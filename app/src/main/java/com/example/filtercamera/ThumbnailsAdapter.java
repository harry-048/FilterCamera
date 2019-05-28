package com.example.filtercamera;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ThumbnailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ThumbnailCallback thumbnailCallback;
    private List<ThumbnailItem> dataSet;
    private List<String> filterName;
    private static int lastPosition = -1;

    public ThumbnailsAdapter(ThumbnailCallback thumbnailCallback, List<ThumbnailItem> dataSet) {
        this.thumbnailCallback = thumbnailCallback;
        this.dataSet = dataSet;
    }

    public ThumbnailsAdapter(List<String> filterName,List<ThumbnailItem> dataSet, ThumbnailCallback thumbnailCallback) {
        this.thumbnailCallback = thumbnailCallback;
        this.dataSet = dataSet;
        this.filterName=filterName;
        Log.d("datasetsize",dataSet.size()+"");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_thumbnail_item, parent, false);
        return new ThumbnailsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final ThumbnailItem thumbnailItem = dataSet.get(position);

        ThumbnailsViewHolder thumbnailsViewHolder = (ThumbnailsViewHolder) holder;
        thumbnailsViewHolder.thumbnail.setImageBitmap(thumbnailItem.image);
        thumbnailsViewHolder.thumnailNameTextView.setText(filterName.get(position));
        Log.d("thumbnailsize","width:"+thumbnailsViewHolder.thumbnail.getWidth()+" ,height"+thumbnailsViewHolder.thumbnail.getHeight());
       // thumbnailsViewHolder.thumbnail.setScaleType(ImageView.ScaleType.FIT_START);
        Log.d("thumbnailsizes","width:"+thumbnailsViewHolder.thumbnail.getWidth()+" ,height"+thumbnailsViewHolder.thumbnail.getHeight());

       //  setAnimation(thumbnailsViewHolder.thumbnail, position);
        thumbnailsViewHolder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastPosition != position) {
                    thumbnailCallback.onThumbnailClick(thumbnailItem.filter);
                    lastPosition = position;
                }
            }

        });
    }

   /* private void setAnimation(View viewToAnimate, int position) {
        {
            ViewHelper.setAlpha(viewToAnimate, .0f);
            com.nineoldandroids.view.ViewPropertyAnimator.animate(viewToAnimate).alpha(1).setDuration(250).start();
            lastPosition = position;
        }
    }
*/
    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
