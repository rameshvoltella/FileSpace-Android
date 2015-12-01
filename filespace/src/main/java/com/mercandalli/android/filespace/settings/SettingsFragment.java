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
package com.mercandalli.android.filespace.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.mercandalli.android.filespace.R;
import com.mercandalli.android.filespace.common.fragment.BackFragment;
import com.mercandalli.android.filespace.common.listener.SetToolbarCallback;
import com.mercandalli.android.filespace.common.util.TimeUtils;
import com.mercandalli.android.filespace.extras.ia.action.ENUM_Action;
import com.mercandalli.android.filespace.main.Constants;
import com.mercandalli.android.filespace.user.LoginRegisterActivity;
import com.mercandalli.android.filespace.welcome.WelcomeActivity;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends BackFragment {

    private static final String BUNDLE_ARG_TITLE = "HomeFragment.Args.BUNDLE_ARG_TITLE";

    private View rootView;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<ModelSetting> list;
    private int click_version;
    private boolean isDevelopper = false;
    private SetToolbarCallback mSetToolbarCallback;
    private String mTitle;
    private Toolbar mToolbar;

    public static SettingsFragment newInstance(String title) {
        final SettingsFragment fragment = new SettingsFragment();
        final Bundle args = new Bundle();
        args.putString(BUNDLE_ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SetToolbarCallback) {
            mSetToolbarCallback = (SetToolbarCallback) context;
        } else {
            throw new IllegalArgumentException("Must be attached to a HomeActivity. Found: " + context);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSetToolbarCallback = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (!args.containsKey(BUNDLE_ARG_TITLE)) {
            throw new IllegalStateException("Missing args. Please use newInstance()");
        }
        mActivity = getActivity();
        mTitle = args.getString(BUNDLE_ARG_TITLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        mToolbar = (Toolbar) rootView.findViewById(R.id.fragment_settings_toolbar);
        mToolbar.setTitle(mTitle);
        mSetToolbarCallback.setToolbar(mToolbar);
        setStatusBarColor(mActivity, R.color.notifications_bar);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        click_version = 0;

        refreshList();

        return rootView;
    }

    public void refreshList() {
        list = new ArrayList<>();
        list.add(new ModelSetting("Settings", Constants.TAB_VIEW_TYPE_SECTION));
        if (mApplicationCallback.getConfig().isLogged()) {
            list.add(new ModelSetting("Auto connection", new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mApplicationCallback.getConfig().setAutoConnection(mActivity, isChecked);
                }
            }, mApplicationCallback.getConfig().isAutoConncetion()));
            list.add(new ModelSetting("Web application", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ENUM_Action.WEB_SEARCH.action.action(mActivity, mApplicationCallback.getConfig().webApplication);
                }
            }));
        }
        list.add(new ModelSetting("Welcome on home screen", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mActivity, "Welcome message enabled.", Toast.LENGTH_SHORT).show();
                mApplicationCallback.getConfig().setHomeWelcomeMessage(mActivity, true);
            }
        }));
        if (mApplicationCallback.getConfig().isLogged()) {
            list.add(new ModelSetting("Change password", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //TODO Change password
                    Toast.makeText(mActivity, getString(R.string.not_implemented), Toast.LENGTH_SHORT).show();
                }
            }));
        }
        if (isDevelopper) {
            list.add(new ModelSetting("Login / Sign in", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mActivity, LoginRegisterActivity.class);
                    mActivity.startActivity(intent);
                    mActivity.overridePendingTransition(R.anim.left_in, R.anim.left_out);
                    mActivity.finish();
                }
            }));
        }

        list.add(new ModelSetting(mActivity.getString(R.string.tutorial), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WelcomeActivity.start(mActivity);
            }
        }));

        list.add(new ModelSetting(mActivity.getString(R.string.about), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogAuthorLabel(mActivity, mApplicationCallback);
            }
        }));

        try {
            PackageInfo pInfo = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            list.add(new ModelSetting("Last update date GMT", TimeUtils.getGMTDate(pInfo.lastUpdateTime)));
            list.add(new ModelSetting("Version", pInfo.versionName, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (click_version == 11) {
                        Toast.makeText(mActivity, "Development settings activated.", Toast.LENGTH_SHORT).show();
                        isDevelopper = true;
                        refreshList();
                    } else if (click_version < 11) {
                        if (click_version >= 1) {
                            final Toast t = Toast.makeText(mActivity, "" + (11 - click_version), Toast.LENGTH_SHORT);
                            t.show();
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    t.cancel();
                                }
                            }, 700);
                        }
                        click_version++;
                    }
                }
            }));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        updateAdapter();
    }

    public void updateAdapter() {
        if (recyclerView != null && list != null) {
            AdapterModelSetting adapter = new AdapterModelSetting(mActivity, mApplicationCallback, list);
            adapter.setOnItemClickListener(new AdapterModelSetting.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    if (position < list.size()) {
                        if (list.get(position).onClickListener != null)
                            list.get(position).onClickListener.onClick(view);
                    }
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public boolean back() {
        return false;
    }

    @Override
    public void onFocus() {

    }
}