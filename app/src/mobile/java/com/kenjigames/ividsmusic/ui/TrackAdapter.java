package com.kenjigames.ividsmusic.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
 * RecyclerView Adapter for binding track items to vertical row layouts (item_track_row.xml).
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private List<Track> tracks = new ArrayList<>();
    private final OnTrackClickListener listener;

    /**
     * Listener interface to dispatch play clicks and action triggers back to host fragments.
     */
    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
        void onActionClick(Track track, int position);
    }

    /**
     * Constructs the TrackAdapter.
     *
     * @param listener The listener interface implementation.
     */
    public TrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the datasets and refreshes views.
     *
     * @param newTracks The new list of tracks.
     */
    public void setTracks(List<Track> newTracks) {
        this.tracks = newTracks != null ? newTracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Gets the current list of tracks loaded in the adapter.
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
                .inflate(R.layout.item_track_row, parent, false);
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
     * ViewHolder pattern implementation wrapping item views.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TextView tvArtist;
        private final ImageButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            btnAction = itemView.findViewById(R.id.btn_action);
        }

        /**
         * Binds the track metadata to the row components.
         */
        public void bind(final Track track, final OnTrackClickListener listener) {
            tvTitle.setText(track.getTitle());
            tvArtist.setText(track.getArtist());

            // Load cover art URL or fallback to drawable using Picasso
            if (track.getCover() != null && !track.getCover().isEmpty()) {
                Picasso.get()
                        .load(track.getCover())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Adjust action button icons dynamically depending on download states
            if (track.isDownloaded()) {
                btnAction.setImageResource(android.R.drawable.ic_menu_delete);
            } else {
                btnAction.setImageResource(android.R.drawable.stat_sys_download);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track, getAdapterPosition());
                }
            });

            btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(track, getAdapterPosition());
                }
            });
        }
    }
}
