/**
 * This file is part of FileSpace for Android, an app for managing your server (files, talks...).
 * <p/>
 * Copyright (c) 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 * <p/>
 * LICENSE:
 * <p/>
 * FileSpace for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p/>
 * FileSpace for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * @author Jonathan Mercandalli
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 */
package com.mercandalli.android.apps.files.file.audio;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mercandalli.android.apps.files.R;
import com.mercandalli.android.apps.files.common.animation.ScaleAnimationAdapter;
import com.mercandalli.android.apps.files.common.fragment.BackFragment;
import com.mercandalli.android.apps.files.file.FileManager;
import com.mercandalli.android.apps.files.file.FileModel;
import com.mercandalli.android.apps.files.file.FileModelCardAdapter;
import com.mercandalli.android.apps.files.file.FileModelCardHeaderItem;
import com.mercandalli.android.apps.files.file.audio.artist.Artist;
import com.mercandalli.android.apps.files.file.audio.artist.ArtistCard;
import com.mercandalli.android.apps.files.file.audio.playlist.AudioPlayList;
import com.mercandalli.android.apps.files.file.audio.playlist.AudioPlayListManager;
import com.mercandalli.android.apps.files.file.local.FileLocalPagerFragment;
import com.mercandalli.android.apps.files.file.local.fab.FileLocalFabManager;
import com.mercandalli.android.apps.files.main.Config;
import com.mercandalli.android.library.base.generic.GenericRecyclerAdapter;
import com.mercandalli.android.library.base.java.StringUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.mercandalli.android.library.base.view.ViewUtils.setViewVisibility;

/**
 * A {@link android.support.v4.app.Fragment} that displays the local {@link FileAudioModel}s.
 */
public class FileAudioLocalFragment extends BackFragment implements
        FileAudioOverflowActions.FileAudioActionCallback,
        FileAudioManager.GetAllLocalMusicListener,
        FileAudioManager.GetLocalMusicFoldersListener,
        FileAudioManager.GetLocalMusicListener,
        FileAudioManager.GetAllLocalMusicArtistsListener,
        FileAudioManager.GetAllLocalMusicAlbumsListener,
        FileAudioManager.MusicsChangeListener,
        FileLocalPagerFragment.ListController,
        FileModelCardAdapter.OnHeaderClickListener,
        FileModelCardAdapter.OnFileSubtitleAdapter,
        ScaleAnimationAdapter.NoAnimatedPosition,
        SwipeRefreshLayout.OnRefreshListener,
        FileAudioModelListener,
        FileLocalPagerFragment.ScrollTop,
        FileLocalFabManager.FabController,
        AudioPlayListManager.GetPlayListsListener {

    private static final String TAG = "FileAudioLocalFragment";

    /**
     * A key for the view pager position.
     */
    private static final String ARG_POSITION_IN_VIEW_PAGER = "FileLocalFragment.Args.ARG_POSITION_IN_VIEW_PAGER";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            PAGE_FOLDERS,
            PAGE_PLAYLIST,
            PAGE_FOLDER_INSIDE,
            PAGE_ARTIST,
            PAGE_ALBUM,
            PAGE_ALL})
    public @interface CurrentPage {
    }

    private static final int PAGE_FOLDERS = 0;
    private static final int PAGE_PLAYLIST = 1;
    private static final int PAGE_FOLDER_INSIDE = 2;
    private static final int PAGE_ARTIST = 3;
    private static final int PAGE_ALBUM = 4;
    private static final int PAGE_ALL = 5;

    @NonNull
    public static FileAudioLocalFragment newInstance(final int positionInViewPager) {
        final FileAudioLocalFragment fileAudioLocalFragment = new FileAudioLocalFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION_IN_VIEW_PAGER, positionInViewPager);
        fileAudioLocalFragment.setArguments(args);
        return fileAudioLocalFragment;
    }

    @CurrentPage
    private int mCurrentPage = PAGE_FOLDERS;

    @NonNull
    private final List<FileModel> mFileModels = new ArrayList<>();
    @NonNull
    private final List<FileAudioModel> mFileAudioModels = new ArrayList<>();
    @NonNull
    private final List<Artist> mArtists = new ArrayList<>();
    @NonNull
    private final List<Album> mAlbums = new ArrayList<>();

    /**
     * A simple {@link Handler}. Called by {@link #showProgressBar()} or {@link #hideProgressBar()}.
     */
    @NonNull
    private final Handler mProgressBarActivationHandler = new Handler();

    /**
     * A simple {@link Runnable}. Called by {@link #showProgressBar()} or {@link #hideProgressBar()}.
     */
    @NonNull
    private final Runnable mProgressBarActivationRunnable;

    @Nullable
    private HeaderView mHeaderView;
    @Nullable
    private RecyclerView mRecyclerView;
    @Nullable
    private TextView mMessageTextView;

    /**
     * A simple {@link ProgressBar}. Call {@link #showProgressBar()} or {@link #hideProgressBar()}.
     */
    @Nullable
    private ProgressBar mProgressBar;

    @Nullable
    private FileAudioRowAdapter mFileAudioRowAdapter;
    @Nullable
    private FileModelCardAdapter mFileModelCardAdapter;

    @Nullable
    private ScaleAnimationAdapter mScaleAnimationAdapter;

    @Nullable
    private String mStringDirectory;
    @Nullable
    private String mStringMusic;
    @Nullable
    private String mStringMusics;

    @Nullable
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int mPositionInViewPager;
    @Nullable
    private FileModel mCurrentFolder;
    @Nullable
    private FileAudioOverflowActions mFileAudioOverflowActions;
    @Nullable
    private FileLocalFabManager mFileLocalFabManager;
    @Nullable
    private FileManager mFileManager;
    @Nullable
    private FileAudioManager mFileAudioManager;
    @Nullable
    private AudioPlayListManager mAudioPlayListManager;

    @NonNull
    private GenericRecyclerAdapter<Album, AlbumCard> mAlbumCardGenericRecyclerAdapter
            = createAlbumCardGenericRecyclerAdapter();

    @NonNull
    private final GenericRecyclerAdapter<Artist, ArtistCard> mArtistCardGenericRecyclerAdapter
            = createArtistCardGenericRecyclerAdapter();

    /**
     * Do not use this constructor. Call {@link #newInstance(int)} instead.
     */
    public FileAudioLocalFragment() {
        mProgressBarActivationRunnable = new Runnable() {
            @Override
            public void run() {
                setViewVisibility(mProgressBar, View.VISIBLE);
                setViewVisibility(mRecyclerView, View.GONE);
            }
        };
    }

    //region Override methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (!args.containsKey(ARG_POSITION_IN_VIEW_PAGER)) {
            throw new IllegalStateException("Missing args. Please use newInstance()");
        }
        final Context context = getContext();
        mFileLocalFabManager = FileLocalFabManager.getInstance();
        mFileManager = FileManager.getInstance(context);
        mFileAudioManager = FileAudioManager.getInstance(context);
        mAudioPlayListManager = AudioPlayListManager.getInstance(context);
        mPositionInViewPager = args.getInt(ARG_POSITION_IN_VIEW_PAGER);
        mFileLocalFabManager.addFabController(mPositionInViewPager, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        mFileLocalFabManager.removeFabController(mPositionInViewPager);
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_file_audio_local, container, false);
        final Context context = getContext();

        mFileAudioOverflowActions = new FileAudioOverflowActions(context, getFragmentManager(), this);

        mStringDirectory = context.getString(R.string.file_audio_model_adapter_directory);
        mStringMusic = context.getString(R.string.file_audio_model_music);
        mStringMusics = context.getString(R.string.file_audio_model_musics);

        mFileAudioManager.addGetAllLocalMusicListener(this);
        mFileAudioManager.addGetLocalMusicFoldersListener(this);
        mFileAudioManager.addGetLocalMusicListener(this);
        mFileAudioManager.addMusicChangeListener(this);
        mFileAudioManager.addGetAllLocalMusicArtistsListener(this);
        mFileAudioManager.addGetAllLocalMusicAlbumsListener(this);
        mAudioPlayListManager.addGetPlayListsListener(this);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.fragment_file_audio_local_progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mMessageTextView = (TextView) rootView.findViewById(R.id.fragment_file_audio_local_message);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.fragment_file_audio_local_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_file_audio_local_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        updateLayoutManager();

        mHeaderView = new HeaderView(context);
        mHeaderView.addOnHeaderClickListener(this);
        mAlbumCardGenericRecyclerAdapter.setHeader(mHeaderView);
        mArtistCardGenericRecyclerAdapter.setHeader(mHeaderView);

        mFileAudioRowAdapter = new FileAudioRowAdapter(context, this, mFileAudioModels, this);
        mFileAudioRowAdapter.setOnItemClickListener(new FileAudioRowAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mFileAudioModels.isEmpty()) {
                    return;
                }
                FileModel fileModel;
                if (position >= mFileAudioModels.size()) {
                    fileModel = mFileAudioModels.get(mFileAudioModels.size() - 1);
                    Log.e(TAG, "onItemClick: position >= size");
                } else {
                    fileModel = mFileAudioModels.get(position);
                }
                if (fileModel.isDirectory()) {
                    refreshListFoldersInside(fileModel);
                } else {
                    mFileManager.execute(
                            (Activity) context,
                            position,
                            new ArrayList<FileModel>(mFileAudioModels),
                            view);
                }
            }
        });

        mFileModelCardAdapter = new FileModelCardAdapter(
                context,
                FileAudioHeaderManager.getInstance().getHeaderIds(),
                this,
                mFileModels,
                null,
                new FileModelCardAdapter.OnFileClickListener() {
                    @Override
                    public void onFileCardClick(View view, int position) {
                        refreshListFoldersInside(mFileModels.get(position));
                    }
                }, null);
        mFileModelCardAdapter.setOnFileSubtitleAdapter(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mFileModelCardAdapter);
            mScaleAnimationAdapter.setDuration(220);
            mScaleAnimationAdapter.setOffsetDuration(32);
            mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
            mRecyclerView.setAdapter(mScaleAnimationAdapter);
        } else {
            mRecyclerView.setAdapter(mFileModelCardAdapter);
        }

        refreshListFolders();

        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }

        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyView() {
        mHeaderView.removeOnHeaderClickListener(this);
        mFileAudioManager.removeGetAllLocalMusicListener(this);
        mFileAudioManager.removeGetLocalMusicFoldersListener(this);
        mFileAudioManager.removeGetLocalMusicListener(this);
        mFileAudioManager.removeMusicChangeListener(this);
        mFileAudioManager.removeGetAllLocalMusicArtistsListener(this);
        mFileAudioManager.removeGetAllLocalMusicAlbumsListener(this);
        mAudioPlayListManager.removeGetPlayListsListener(this);
        super.onDestroyView();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeFileAudioModel(final FileAudioModel fileAudioModel, final View view) {
        mFileAudioOverflowActions.show(fileAudioModel, view, Config.isLogged());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshData() {
        refreshCurrentList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean back() {
        if (mCurrentPage == PAGE_FOLDER_INSIDE) {
            refreshListFolders();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFabClick(
            final @IntRange(from = 0, to = FileLocalFabManager.NUMBER_MAX_OF_FAB - 1) int fabPosition,
            final @NonNull FloatingActionButton floatingActionButton) {
        if (fabPosition == 0) {
            refreshListFolders();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFabVisible(
            @IntRange(from = 0, to = FileLocalFabManager.NUMBER_MAX_OF_FAB - 1) final int fabPosition) {
        return fabPosition == 0 && mCurrentPage == PAGE_FOLDER_INSIDE;
    }

    /**
     * {@inheritDoc}
     */
    @DrawableRes
    @Override
    public int getFabImageResource(
            @IntRange(from = 0, to = FileLocalFabManager.NUMBER_MAX_OF_FAB - 1) final int fabPosition) {
        return R.drawable.arrow_up;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public String onFileSubtitleModify(final FileModel fileModel) {
        if (fileModel != null && fileModel.isDirectory() && fileModel.getCountAudio() != 0) {
            return mStringDirectory + ": " + StringUtils.longToShortString(fileModel.getCountAudio())
                    + " " + (fileModel.getCountAudio() > 1 ? mStringMusics : mStringMusic);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onHeaderClick(View v, List<FileModelCardHeaderItem> fileModelCardHeaderItems) {
        FileAudioHeaderManager.getInstance().setHeaderIds(fileModelCardHeaderItems);
        final int viewId = v.getId();
        switch (viewId) {
            case R.id.view_file_header_audio_folder:
                refreshListFolders();
                break;
            case R.id.view_file_header_audio_playlist:
                refreshListPlaylist();
                break;
            case R.id.view_file_header_audio_recent:
                //TODO
                break;
            case R.id.view_file_header_audio_artist:
                refreshListArtist();
                break;
            case R.id.view_file_header_audio_album:
                refreshListAlbum();
                break;
            case R.id.view_file_header_audio_all:
                refreshListAllMusic();
                break;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRefresh() {
        refreshCurrentList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnimatedItem(int position) {
        return mCurrentPage == PAGE_FOLDER_INSIDE || position != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMusicsContentChange() {
        refreshCurrentList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshCurrentList() {
        if (mFileAudioManager == null || mAudioPlayListManager == null) {
            return;
        }
        showProgressBar();
        switch (mCurrentPage) {
            case PAGE_ALL:
                mFileAudioManager.getAllLocalMusic();
                break;
            case PAGE_PLAYLIST:
                mAudioPlayListManager.getPlayLists();
                break;
            case PAGE_FOLDERS:
                mFileAudioManager.getLocalMusicFolders();
                break;
            case PAGE_FOLDER_INSIDE:
                mFileAudioManager.getLocalMusic(mCurrentFolder);
                break;
            case PAGE_ALBUM:
                mFileAudioManager.getAllLocalMusicAlbums();
                break;
            case PAGE_ARTIST:
                mFileAudioManager.getAllLocalMusicArtists();
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAdapter() {
        if (mRecyclerView != null && isAdded()) {
            mSwipeRefreshLayout.setRefreshing(false);
            mFileLocalFabManager.updateFabButtons();

            if (isEmpty()) {
                mMessageTextView.setText(getString(R.string.no_music));
                mMessageTextView.setVisibility(View.VISIBLE);
            } else {
                mMessageTextView.setVisibility(View.GONE);
            }

            if (mCurrentPage == PAGE_FOLDERS) {
                mFileModelCardAdapter.setList(mFileModels);
            } else if (mCurrentPage == PAGE_ALBUM) {
                mAlbumCardGenericRecyclerAdapter.setModels(mAlbums);
            } else if (mCurrentPage == PAGE_ARTIST) {
                mArtistCardGenericRecyclerAdapter.setModels(mArtists);
            } else {
                mFileAudioRowAdapter.setList(mFileAudioModels);
            }

            updateLayoutManager();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicSucceeded(final List<FileAudioModel> fileModels) {
        if (mCurrentPage != PAGE_ALL) {
            return;
        }
        hideProgressBar();

        mFileAudioModels.clear();
        mFileAudioModels.addAll(fileModels);
        mFileAudioRowAdapter.setHasHeader(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mFileAudioRowAdapter);
            mScaleAnimationAdapter.setDuration(220);
            mScaleAnimationAdapter.setOffsetDuration(32);
            mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
            mRecyclerView.setAdapter(mScaleAnimationAdapter);
        } else {
            mRecyclerView.setAdapter(mFileAudioRowAdapter);
        }
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicFailed() {
        if (mCurrentPage != PAGE_ALL) {
            return;
        }
        hideProgressBar();
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGetPlayListsSucceeded(@NonNull final List<AudioPlayList> audioPlayLists) {
        if (mCurrentPage != PAGE_PLAYLIST) {
            return;
        }
        hideProgressBar();

        mFileAudioModels.clear();
        mFileAudioRowAdapter.setHasHeader(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mFileAudioRowAdapter);
            mScaleAnimationAdapter.setDuration(220);
            mScaleAnimationAdapter.setOffsetDuration(32);
            mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
            mRecyclerView.setAdapter(mScaleAnimationAdapter);
        } else {
            mRecyclerView.setAdapter(mFileAudioRowAdapter);
        }
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGetPlayListsFailed() {
        if (mCurrentPage != PAGE_PLAYLIST) {
            return;
        }
        hideProgressBar();
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLocalMusicFoldersSucceeded(final List<FileModel> fileModels) {
        if (mCurrentPage != PAGE_FOLDERS) {
            return;
        }
        hideProgressBar();
        mFileModels.clear();
        mFileModels.addAll(fileModels);
        mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mFileModelCardAdapter);
        mScaleAnimationAdapter.setDuration(220);
        mScaleAnimationAdapter.setOffsetDuration(32);
        mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
        mRecyclerView.setAdapter(mScaleAnimationAdapter);
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLocalMusicFoldersFailed() {
        if (mCurrentPage != PAGE_FOLDERS) {
            return;
        }
        hideProgressBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLocalMusicSucceeded(final List<FileAudioModel> fileModels) {
        if (mCurrentPage != PAGE_FOLDER_INSIDE) {
            return;
        }
        hideProgressBar();

        mFileAudioModels.clear();
        mFileAudioModels.addAll(fileModels);
        mFileAudioRowAdapter.setHasHeader(false);

        mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mFileAudioRowAdapter);
        mScaleAnimationAdapter.setDuration(220);
        mScaleAnimationAdapter.setOffsetDuration(32);
        mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
        mRecyclerView.setAdapter(mScaleAnimationAdapter);
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLocalMusicFailed() {
        if (mCurrentPage != PAGE_FOLDER_INSIDE) {
            return;
        }
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicArtistsSucceeded(final List<Artist> artists) {
        if (mCurrentPage != PAGE_ARTIST) {
            return;
        }
        hideProgressBar();
        mArtists.clear();
        mArtists.addAll(artists);
        mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mArtistCardGenericRecyclerAdapter);
        mScaleAnimationAdapter.setDuration(220);
        mScaleAnimationAdapter.setOffsetDuration(32);
        mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
        mRecyclerView.setAdapter(mScaleAnimationAdapter);
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicArtistsFailed() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicAlbumsSucceeded(final List<Album> albums) {
        if (mCurrentPage != PAGE_ALBUM) {
            return;
        }
        hideProgressBar();
        mAlbums.clear();
        mAlbums.addAll(albums);
        mScaleAnimationAdapter = new ScaleAnimationAdapter(mRecyclerView, mAlbumCardGenericRecyclerAdapter);
        mScaleAnimationAdapter.setDuration(220);
        mScaleAnimationAdapter.setOffsetDuration(32);
        mScaleAnimationAdapter.setNoAnimatedPosition(FileAudioLocalFragment.this);
        mRecyclerView.setAdapter(mScaleAnimationAdapter);
        updateAdapter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAllLocalMusicAlbumsFailed() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scrollTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }
    //endregion Override methods

    //region refresh
    private void refreshListFolders() {
        mCurrentFolder = null;
        mCurrentPage = PAGE_FOLDERS;
        showProgressBar();
        refreshCurrentList();
    }

    private void refreshListPlaylist() {
        mCurrentPage = PAGE_PLAYLIST;
        showProgressBar();
        refreshCurrentList();
    }

    private void refreshListAllMusic() {
        mCurrentPage = PAGE_ALL;
        showProgressBar();
        refreshCurrentList();
    }

    private void refreshListArtist() {
        mCurrentPage = PAGE_ARTIST;
        showProgressBar();
        refreshCurrentList();
    }

    private void refreshListAlbum() {
        mCurrentPage = PAGE_ALBUM;
        showProgressBar();
        refreshCurrentList();
    }

    private void refreshListFoldersInside(final FileModel fileModel) {
        mCurrentFolder = fileModel;
        mCurrentPage = PAGE_FOLDER_INSIDE;
        mFileAudioModels.clear();
        refreshCurrentList();
    }
    //endregion refresh

    private void updateLayoutManager() {
        if (mCurrentPage == PAGE_FOLDERS || mCurrentPage == PAGE_ALBUM) {
            final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),
                    getResources().getInteger(R.integer.column_number_small_card));
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return mFileModelCardAdapter.isHeader(position) ?
                            gridLayoutManager.getSpanCount() : 1;
                }
            });
            mRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            final int nbColumn = getResources().getInteger(R.integer.column_number_card);
            if (nbColumn <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            } else {
                final GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), nbColumn);
                mRecyclerView.setLayoutManager(gridLayoutManager);
                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return mFileAudioRowAdapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
                    }
                });
            }
        }
    }

    private void showProgressBar() {
        mProgressBarActivationHandler.postDelayed(mProgressBarActivationRunnable, 200);
    }

    private void hideProgressBar() {
        mProgressBarActivationHandler.removeCallbacks(mProgressBarActivationRunnable);
        mProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private boolean isEmpty() {
        switch (mCurrentPage) {
            case PAGE_ALL:
                return mFileAudioModels.isEmpty();
            case PAGE_FOLDERS:
                return mFileModels.isEmpty();
            case PAGE_FOLDER_INSIDE:
                return mFileModels.isEmpty();
            case PAGE_ALBUM:
                return mAlbums.isEmpty();
            case PAGE_ARTIST:
                return mArtists.isEmpty();
            case PAGE_PLAYLIST:
                return mFileAudioModels.isEmpty();
            default:
                return mFileAudioModels.isEmpty();
        }
    }

    @NonNull
    private GenericRecyclerAdapter<Album, AlbumCard> createAlbumCardGenericRecyclerAdapter() {
        return new GenericRecyclerAdapter<>(new GenericRecyclerAdapter.ViewFabric<AlbumCard>() {
            @NonNull
            @Override
            public AlbumCard newInstance(@NonNull final Context context) {
                return new AlbumCard(context);
            }
        });
    }

    @NonNull
    private GenericRecyclerAdapter<Artist, ArtistCard> createArtistCardGenericRecyclerAdapter() {
        return new GenericRecyclerAdapter<>(new GenericRecyclerAdapter.ViewFabric<ArtistCard>() {
            @NonNull
            @Override
            public ArtistCard newInstance(@NonNull final Context context) {
                return new ArtistCard(context);
            }
        });
    }
}
