package com.mercandalli.android.apps.files.file.audio.artist;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.mercandalli.android.apps.files.R;
import com.mercandalli.android.library.base.precondition.Preconditions;
import com.mercandalli.android.library.base.view.GenericRecyclerAdapter;

public class ArtistCard extends CardView implements View.OnClickListener, GenericRecyclerAdapter.GenericView<Artist> {

    private Artist mArtist;

    /**
     * An {@link OnArtistCardClickListener} for click.
     */
    private OnArtistCardClickListener mOnArtistCardClickListener;

    public ArtistCard(Context context) {
        super(context);
        init(context);
    }

    public ArtistCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArtistCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void onClick(View v) {
        if (mOnArtistCardClickListener != null && mArtist != null) {
            mOnArtistCardClickListener.onArtistCardClicked(mArtist, v);
        }
    }

    public void setOnArtistCardClickListener(final OnArtistCardClickListener onArtistCardClickListener) {
        Preconditions.checkNotNull(onArtistCardClickListener);
        mOnArtistCardClickListener = onArtistCardClickListener;
    }

    /**
     * Initialize this {@link ArtistCard}.
     *
     * @param context the {@link Context} passed in the constructor.
     */
    private void init(Context context) {
        final View rootView = inflate(context, R.layout.file_audio_artist_card, this);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setUseCompatPadding(true);
        setCardBackgroundColor(Color.WHITE);
        findViews(rootView);
        initViews(rootView);
    }

    /**
     * Find the different{@link View}s used in this {@link ArtistCard}.
     *
     * @param rootView the root {@link View} inflated.
     */
    private void findViews(View rootView) {

    }

    private void initViews(View rootView) {
        setOnClickListener(this);
    }

    @NonNull
    @Override
    public GenericRecyclerAdapter.GenericView<Artist> newInstance(@NonNull final Context context) {
        return new ArtistCard(context);
    }

    @Override
    public void setModel(final Artist artist) {
        mArtist = artist;
    }

    interface OnArtistCardClickListener {
        void onArtistCardClicked(Artist artist, View v);
    }
}
