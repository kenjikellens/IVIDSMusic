package com.kenjigames.ividsmusic.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kenjigames.ividsmusic.R;
import com.kenjigames.ividsmusic.data.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for binding track items to CardView card layouts (item_track_card.xml).
 */
public class TrackCardAdapter extends RecyclerView.Adapter<TrackCardAdapter.ViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private final OnCardClickListener listener;

    /**
     * Interface to dispatch click callbacks.
     */
    public interface OnCardClickListener {
        void onCardClick(Track track, int position);
    }

    /**
     * Constructs the TrackCardAdapter.
     *
     * @param listener The card click listener.
     */
    public TrackCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the dataset and triggers an update of all card view widgets.
     *
     * @param newTracks The new list of tracks.
     */
    public void setTracks(List<Track> newTracks) {
        this.tracks = newTracks != null ? newTracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Gets the list of tracks loaded in the card adapter.
     *
     * @return The list of tracks.
     */
    public List<Track> getTracks() {
        return tracks;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track, listener);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    /**
     * ViewHolder container wrapping layout elements.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TextView tvArtist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_card_cover);
            tvTitle = itemView.findViewById(R.id.tv_card_title);
            tvArtist = itemView.findViewById(R.id.tv_card_artist);
        }

        /**
         * Renders the track data into the card views.
         */
        public void bind(final Track track, final OnCardClickListener listener) {
            tvTitle.setText(track.getTitle());
            tvArtist.setText(track.getArtist());

            // Image URL loader with placeholders
            if (track.getCover() != null && !track.getCover().isEmpty()) {
                Picasso.get()
                        .load(track.getCover())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCardClick(track, getAdapterPosition());
                }
            });
        }
    }
}
