/**
 * This file is part of FileSpace for Android, an app for managing your server (files, talks...).
 *
 * Copyright (c) 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 *
 * LICENSE:
 *
 * FileSpace for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * FileSpace for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * @author Jonathan Mercandalli
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 */
package mercandalli.com.filespace.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import mercandalli.com.filespace.R;
import mercandalli.com.filespace.util.GpsUtils;
import mercandalli.com.filespace.util.HashUtils;
import mercandalli.com.filespace.ui.activity.ActivityMain;
import mercandalli.com.filespace.ui.activity.Application;
import mercandalli.com.filespace.listener.IPostExecuteListener;
import mercandalli.com.filespace.model.ModelUser;
import mercandalli.com.filespace.net.TaskPost;
import mercandalli.com.filespace.util.StringPair;
import mercandalli.com.filespace.util.StringUtils;

import static mercandalli.com.filespace.util.NetUtils.isInternetConnection;

public class InscriptionFragment extends Fragment {

	private Application app;

    private boolean requestLaunched = false; // Block the second task if one launch

    EditText username, password;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.app = (Application) activity;
    }

	public InscriptionFragment() {
		super();
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inscription, container, false);
        this.username = (EditText) rootView.findViewById(R.id.username);
        this.password = (EditText) rootView.findViewById(R.id.password);

        ((CheckBox) rootView.findViewById(R.id.autoconnection)).setChecked(app.getConfig().isAutoConncetion());
        ((CheckBox) rootView.findViewById(R.id.autoconnection)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                app.getConfig().setAutoConnection(isChecked);
            }
        });

        this.username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    InscriptionFragment.this.password.requestFocus();
                    return true;
                }
                return false;
            }
        });

        this.password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    inscription();
                    return true;
                }
                return false;
            }
        });

        return rootView;
    }

    public void connectionSucceed() {
        Intent intent = new Intent(getActivity(), ActivityMain.class);
        this.startActivity(intent);
        getActivity().overridePendingTransition(R.anim.left_in, R.anim.left_out);
        getActivity().finish();
    }

    public void inscription() {
        ModelUser user = new ModelUser();

        if (!StringUtils.isNullOrEmpty(username.getText().toString()))
            user.username = username.getText().toString();

        if (!StringUtils.isNullOrEmpty(password.getText().toString()))
            user.password = HashUtils.sha1(password.getText().toString());

        inscription(user);
    }

    public void inscription(ModelUser user) {
        if (requestLaunched)
            return;
        requestLaunched = true;

        if (!StringUtils.isNullOrEmpty(user.username))
            app.getConfig().setUserUsername(user.username);
        else
            user.username = app.getConfig().getUserUsername();

        if (!StringUtils.isNullOrEmpty(user.password))
            app.getConfig().setUserPassword(user.password);
        else
            user.password = app.getConfig().getUserPassword();

        if (StringUtils.isNullOrEmpty(app.getConfig().getUrlServer())) {
            requestLaunched = false;
            return;
        }

        // Register : POST /user
        List<StringPair> parameters = new ArrayList<>();
        parameters.add(new StringPair("username", "" + user.username));
        parameters.add(new StringPair("password", "" + user.password));
        parameters.add(new StringPair("latitude", "" + GpsUtils.getLatitude(getActivity())));
        parameters.add(new StringPair("longitude", "" + GpsUtils.getLongitude(getActivity())));
        parameters.add(new StringPair("altitude", "" + GpsUtils.getAltitude(getActivity())));

        if(isInternetConnection(app))
            (new TaskPost(app, app.getConfig().getUrlServer() + app.getConfig().routeUser, new IPostExecuteListener() {
                @Override
                public void execute(JSONObject json, String body) {
                    try {
                        if (json != null) {
                            if (json.has("succeed")) {
                                if (json.getBoolean("succeed"))
                                    connectionSucceed();
                            }
                            if (json.has("user")) {
                                JSONObject user = json.getJSONObject("user");
                                if (user.has("id"))
                                    app.getConfig().setUserId(user.getInt("id"));
                            }
                        } else
                            Toast.makeText(app, app.getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    requestLaunched = false;
                }
            }, parameters)).execute();
        else
            requestLaunched = false;
    }
}