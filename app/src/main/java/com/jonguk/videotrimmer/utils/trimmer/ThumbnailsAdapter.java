package com.jonguk.videotrimmer.utils.trimmer;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonguk on 2018. 1. 3..
 */

class ThumbnailsAdapter extends RecyclerView.Adapter<ThumbnailsAdapter.ThumbnailViewHolder> {

    private final int mViewHolderWidth;
    private final List<Bitmap> mItems = new ArrayList<>();

    public ThumbnailsAdapter(int viewHolderWidth) {
        mViewHolderWidth = viewHolderWidth;
    }

    void setItems(ArrayList<Bitmap> list) {
        mItems.clear();
        mItems.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView v = new ImageView(parent.getContext());
        v.setLayoutParams(new ViewGroup.LayoutParams(mViewHolderWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ThumbnailViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        ImageView itemView = (ImageView) holder.itemView;
        Glide.with(itemView)
                .load(mItems.get(position))
                .into(itemView);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class ThumbnailViewHolder extends RecyclerView.ViewHolder {
        ThumbnailViewHolder(View itemView) {
            super(itemView);
        }
    }
}
