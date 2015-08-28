/**
 * This file is part of Jarvis for Android, an app for managing your server (files, talks...).
 *
 * Copyright (c) 2014-2015 Jarvis for Android contributors (http://mercandalli.com)
 *
 * LICENSE:
 *
 * Jarvis for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Jarvis for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * @author Jonathan Mercandalli
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2014-2015 Jarvis for Android contributors (http://mercandalli.com)
 */
package mercandalli.com.filespace.ui.fragment.genealogy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import mercandalli.com.filespace.R;
import mercandalli.com.filespace.listener.IListener;
import mercandalli.com.filespace.listener.IModelGenealogyUserListener;
import mercandalli.com.filespace.listener.IPostExecuteListener;
import mercandalli.com.filespace.model.ModelGenealogyUser;
import mercandalli.com.filespace.net.TaskGet;
import mercandalli.com.filespace.ui.activity.Application;
import mercandalli.com.filespace.ui.adapter.AdapterModelGenealogyUser;
import mercandalli.com.filespace.ui.dialog.DialogAddGenealogyUser;
import mercandalli.com.filespace.ui.fragment.Fragment;
import mercandalli.com.filespace.ui.view.DividerItemDecoration;
import mercandalli.com.filespace.util.StringPair;

import static mercandalli.com.filespace.util.NetUtils.isInternetConnection;

/**
 * Created by Jonathan on 28/08/2015.
 */
public class GenealogyListFragment extends Fragment {

    private Application app;
    private View rootView;

    List<ModelGenealogyUser> list;
    private RecyclerView recyclerView;
    private AdapterModelGenealogyUser mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar circularProgressBar;
    private TextView message;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (Application) activity;
    }

    public GenealogyListFragment() {
        super();
    }

    public GenealogyListFragment(Application app) {
        this.app = app;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_genealogy_list, container, false);
        this.circularProgressBar = (ProgressBar) rootView.findViewById(R.id.circularProgressBar);
        this.message = (TextView) rootView.findViewById(R.id.message);

        ((ImageButton) this.rootView.findViewById(R.id.circle)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });

        this.recyclerView = (RecyclerView) rootView.findViewById(R.id.listView);
        this.recyclerView.setHasFixedSize(true);
        this.mLayoutManager = new LinearLayoutManager(getActivity());
        this.recyclerView.setLayoutManager(mLayoutManager);
        this.recyclerView.setItemAnimator(/*new SlideInFromLeftItemAnimator(mRecyclerView)*/new DefaultItemAnimator());
        this.recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        refreshList();

        return rootView;
    }

    public void refreshList() {
        refreshList(null);
    }

    public void refreshList(String search) {
        List<StringPair> parameters = null;
        if(isInternetConnection(app) && app.isLogged())
            new TaskGet(
                    app,
                    this.app.getConfig().getUser(),
                    this.app.getConfig().getUrlServer() + this.app.getConfig().routeGenealogy,
                    new IPostExecuteListener() {
                        @Override
                        public void execute(JSONObject json, String body) {
                            list = new ArrayList<>();
                            try {
                                if (json != null) {
                                    if (json.has("result")) {
                                        JSONArray array = json.getJSONArray("result");
                                        for (int i = 0; i < array.length(); i++) {
                                            ModelGenealogyUser modelUser = new ModelGenealogyUser(app, array.getJSONObject(i));
                                            list.add(modelUser);
                                        }
                                    }
                                }
                                else
                                    Toast.makeText(app, app.getString(R.string.action_failed), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            updateAdapter();
                        }
                    },
                    parameters
            ).execute();
        else {
            this.circularProgressBar.setVisibility(View.GONE);
            this.message.setText(app.isLogged()?getString(R.string.no_internet_connection):getString(R.string.no_logged));
            this.message.setVisibility(View.VISIBLE);
            this.swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void updateAdapter() {
        if(this.recyclerView!=null && this.list!=null && this.isAdded()) {
            this.circularProgressBar.setVisibility(View.GONE);

            if(this.list.size()==0) {
                this.message.setText(getString(R.string.no_user));
                this.message.setVisibility(View.VISIBLE);
            }
            else
                this.message.setVisibility(View.GONE);

            this.mAdapter = new AdapterModelGenealogyUser(app, list, new IModelGenealogyUserListener() {
                @Override
                public void execute(final ModelGenealogyUser modelGenealogyUser) {

                    final AlertDialog.Builder menuAlert = new AlertDialog.Builder(app);
                    String[] menuList = { getString(R.string.modify), getString(R.string.delete), getString(R.string.properties) };
                    menuAlert.setTitle("Action");
                    menuAlert.setItems(menuList,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int item) {
                                    switch (item) {
                                        case 0:
                                            modelGenealogyUser.modify(new IPostExecuteListener() {
                                                @Override
                                                public void execute(JSONObject json, String body) {
                                                    refreshList();
                                                }
                                            });
                                            break;
                                        case 1:
                                            GenealogyListFragment.this.app.alert("Delete", "Delete " + (modelGenealogyUser.first_name_1) + " ?", "Yes", new IListener() {
                                                @Override
                                                public void execute() {
                                                    modelGenealogyUser.delete(new IPostExecuteListener() {
                                                        @Override
                                                        public void execute(JSONObject json, String body) {
                                                            refreshList();
                                                        }
                                                    });
                                                }
                                            }, "No", null);
                                            break;
                                        case 2:
                                            GenealogyListFragment.this.app.alert(
                                                    getString(R.string.data) + " : " + modelGenealogyUser.first_name_1,
                                                    modelGenealogyUser.toSpanned(),
                                                    "OK",
                                                    null,
                                                    null,
                                                    null);
                                            break;

                                    }
                                }
                            });
                    AlertDialog menuDrop = menuAlert.create();
                    menuDrop.show();

                }
            });
            this.recyclerView.setAdapter(mAdapter);

            this.mAdapter.setOnItemClickListener(new AdapterModelGenealogyUser.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    GenealogyListFragment.this.app.alert(
                            getString(R.string.data) + " : " + list.get(position).first_name_1,
                            list.get(position).toSpanned(),
                            "OK",
                            null,
                            null,
                            null);
                }
            });

            this.swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public boolean back() {
        return false;
    }

    public void add() {
        app.dialog = new DialogAddGenealogyUser(app,new IPostExecuteListener() {
            @Override
            public void execute(JSONObject json, String body) {
                refreshList();
            }
        });
    }
}