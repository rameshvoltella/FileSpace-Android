/**
 * This file is part of Jarvis for Android, an app for managing your server (files, talks...).
 * <p/>
 * Copyright (c) 2014-2015 Jarvis for Android contributors (http://mercandalli.com)
 * <p/>
 * LICENSE:
 * <p/>
 * Jarvis for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p/>
 * Jarvis for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * @author Jonathan Mercandalli
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2014-2015 Jarvis for Android contributors (http://mercandalli.com)
 */
package mercandalli.com.filespace.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import mercandalli.com.filespace.R;
import mercandalli.com.filespace.listeners.ILocationListener;
import mercandalli.com.filespace.listeners.IPostExecuteListener;
import mercandalli.com.filespace.models.ModelSetting;
import mercandalli.com.filespace.models.ModelUser;
import mercandalli.com.filespace.net.TaskGet;
import mercandalli.com.filespace.net.TaskPost;
import mercandalli.com.filespace.ui.adapters.AdapterModelSetting;
import mercandalli.com.filespace.utils.FileUtils;
import mercandalli.com.filespace.utils.FontUtils;
import mercandalli.com.filespace.utils.GpsUtils;
import mercandalli.com.filespace.utils.ImageUtils;
import mercandalli.com.filespace.utils.StringPair;
import mercandalli.com.filespace.utils.StringUtils;
import mercandalli.com.filespace.utils.TimeUtils;

import static mercandalli.com.filespace.utils.NetUtils.isInternetConnection;

/**
 * Created by Jonathan on 03/01/2015.
 */
public class ProfileFragment extends BackFragment {

    private View rootView;
    private ProgressBar circularProgressBar;
    private ModelUser user;

    private TextView username;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<ModelSetting> list = new ArrayList<>();

    private ImageView icon_back;

    public static ProfileFragment newInstance() {
        Bundle args = new Bundle();
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        this.circularProgressBar = (ProgressBar) this.rootView.findViewById(R.id.circularProgressBar);
        this.circularProgressBar.setVisibility(View.VISIBLE);

        icon_back = (ImageView) rootView.findViewById(R.id.icon_back);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);

        Bitmap icon_profile_online = app.getConfig().getUserProfilePicture();
        if (icon_profile_online != null) {
            icon_back.setImageBitmap(ImageUtils.setBlur(ImageUtils.setBrightness(icon_profile_online, -50), 15));
        }

        this.username = (TextView) this.rootView.findViewById(R.id.username);
        this.username.setText(StringUtils.capitalize(app.getConfig().getUserUsername()));
        FontUtils.applyFont(app, this.username, "fonts/Roboto-Regular.ttf");

        refreshView();

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Drawable drawable = icon_back.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmapDrawable.getBitmap().recycle();
        }
    }

    @Override
    public boolean back() {
        return false;
    }

    @Override
    public void onFocus() {

    }

    public void refreshView() {
        if (isInternetConnection(app) && app.isLogged()) {
            List<StringPair> parameters = null;
            new TaskGet(
                    app,
                    this.app.getConfig().getUser(),
                    this.app.getConfig().getUrlServer() + this.app.getConfig().routeUser + "/" + this.app.getConfig().getUserId(),
                    new IPostExecuteListener() {
                        @Override
                        public void execute(JSONObject json, String body) {
                            if (!isAdded())
                                return;
                            try {
                                if (json != null) {
                                    if (json.has("result")) {
                                        user = new ModelUser(app, json.getJSONObject("result"));
                                        list.clear();
                                        list.add(new ModelSetting(app, "Username", "" + user.username));
                                        list.add(new ModelSetting(app, "Files size", FileUtils.humanReadableByteCount(user.size_files) + " / " + FileUtils.humanReadableByteCount(user.server_max_size_end_user)));
                                        list.add(new ModelSetting(app, "Files count", "" + user.num_files));
                                        list.add(new ModelSetting(app, "Creation date", "" + TimeUtils.getDate(user.date_creation)));
                                        list.add(new ModelSetting(app, "Connection date", "" + TimeUtils.getDate(user.date_last_connection)));
                                        if (user.isAdmin()) {
                                            list.add(new ModelSetting(app, "Admin", "" + user.isAdmin()));

                                            if (user.userLocation != null) {
                                                list.add(new ModelSetting(app, "Longitude", "" + user.userLocation.longitude));
                                                list.add(new ModelSetting(app, "Latitude", "" + user.userLocation.latitude));
                                                list.add(new ModelSetting(app, "Altitude", "" + user.userLocation.altitude));
                                            }
                                        }

                                        Location location = GpsUtils.getGpsLocation(app, new ILocationListener() {
                                            @Override
                                            public void execute(Location location) {
                                                if (location != null) {
                                                    double longitude = location.getLongitude(),
                                                            latitude = location.getLatitude();

                                                    list.add(new ModelSetting(app, "Gps Longitude", "" + longitude));
                                                    list.add(new ModelSetting(app, "Gps Latitude", "" + latitude));

                                                    if (isInternetConnection(app) && longitude != 0 && latitude != 0) {
                                                        List<StringPair> parameters = new ArrayList<>();
                                                        parameters.add(new StringPair("longitude", "" + longitude));
                                                        parameters.add(new StringPair("latitude", "" + latitude));

                                                        (new TaskPost(app, app.getConfig().getUrlServer() + app.getConfig().routeUserPut, new IPostExecuteListener() {
                                                            @Override
                                                            public void execute(JSONObject json, String body) {

                                                            }
                                                        }, parameters)).execute();
                                                    }
                                                }
                                            }
                                        });

                                        if (location != null) {
                                            double longitude = location.getLongitude(),
                                                    latitude = location.getLatitude();

                                            list.add(new ModelSetting(app, "Gps Longitude", "" + longitude));
                                            list.add(new ModelSetting(app, "Gps Latitude", "" + latitude));

                                            if (isInternetConnection(app) && longitude != 0 && latitude != 0) {
                                                List<StringPair> parameters = new ArrayList<>();
                                                parameters.add(new StringPair("longitude", "" + longitude));
                                                parameters.add(new StringPair("latitude", "" + latitude));

                                                (new TaskPost(app, app.getConfig().getUrlServer() + app.getConfig().routeUserPut, new IPostExecuteListener() {
                                                    @Override
                                                    public void execute(JSONObject json, String body) {

                                                    }
                                                }, parameters)).execute();
                                            }
                                        }
                                    }
                                } else
                                    Toast.makeText(app, app.getString(R.string.action_failed), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            updateView();
                        }
                    },
                    parameters
            ).execute();
        }
    }

    public void updateView() {
        this.circularProgressBar.setVisibility(View.GONE);

        if (recyclerView != null && list != null) {
            AdapterModelSetting adapter = new AdapterModelSetting(app, list);
            adapter.setOnItemClickListener(new AdapterModelSetting.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    if (position < list.size()) {
                        switch (position) {
                        }

                    }
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }
}