/**
 * Personal Project : Control server
 *
 * MERCANDALLI Jonathan
 */

package mercandalli.com.jarvis.fragment;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import mercandalli.com.jarvis.R;
import mercandalli.com.jarvis.activity.Application;
import mercandalli.com.jarvis.adapter.AdapterModelFile;
import mercandalli.com.jarvis.dialog.DialogAddFileManager;
import mercandalli.com.jarvis.listener.IListener;
import mercandalli.com.jarvis.listener.IModelFileListener;
import mercandalli.com.jarvis.listener.IPostExecuteListener;
import mercandalli.com.jarvis.listener.IStringListener;
import mercandalli.com.jarvis.model.ModelFile;
import mercandalli.com.jarvis.net.TaskGet;
import mercandalli.com.jarvis.view.DividerItemDecoration;


public class FileManagerFragmentCloud extends Fragment {

	private Application app;
	private RecyclerView listView;
    private RecyclerView.LayoutManager mLayoutManager;
    private AdapterModelFile adapter;
    private ArrayList<ModelFile> files = new ArrayList<>();
	private ProgressBar circularProgressBar;
	private TextView message;
	private SwipeRefreshLayout swipeRefreshLayout;
    Animation animOpen; ImageButton circle, circle2;

    private String url = "";
    private List<ModelFile> filesToCut = new ArrayList<>();

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        app = (Application) activity;
    }

	public FileManagerFragmentCloud() {
		super();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_filemanager_files, container, false);
        this.circularProgressBar = (ProgressBar) rootView.findViewById(R.id.circulerProgressBar);
        this.message = (TextView) rootView.findViewById(R.id.message);

        this.listView = (RecyclerView) rootView.findViewById(R.id.listView);
        this.listView.setHasFixedSize(true);
        this.mLayoutManager = new LinearLayoutManager(getActivity());
        this.listView.setLayoutManager(mLayoutManager);

        this.swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);

        this.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshList();
			}
		});

        this.circle = ((ImageButton) rootView.findViewById(R.id.circle));
        this.circle.setVisibility(View.GONE);
        this.animOpen = AnimationUtils.loadAnimation(this.app, R.anim.circle_button_bottom_open);

        this.circle2 = ((ImageButton) rootView.findViewById(R.id.circle2));
        this.circle2.setVisibility(View.GONE);

        this.circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileManagerFragmentCloud.this.app.dialog = new DialogAddFileManager(app, -1, new IPostExecuteListener() {
                    @Override
                    public void execute(JSONObject json, String body) {
                    if (json != null)
                        refreshList();
                    }
                });
            }
        });

        this.circle2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileManagerFragmentCloud.this.url = "";
                FileManagerFragmentCloud.this.refreshList();
            }
        });

        this.adapter = new AdapterModelFile(app, files, new IModelFileListener() {
            @Override
            public void execute(final ModelFile modelFile) {
                final AlertDialog.Builder menuAleart = new AlertDialog.Builder(FileManagerFragmentCloud.this.app);
                String[] menuList = { getString(R.string.download) };
                if(!modelFile.directory && modelFile.isMine())
                    menuList = new String[] { getString(R.string.download), getString(R.string.rename), getString(R.string.delete), getString(R.string.cut), (modelFile.public_) ? "Become private" : "Become public" };
                menuAleart.setTitle(getString(R.string.action));
                menuAleart.setItems(menuList,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                switch (item) {
                                    case 0:
                                        modelFile.download(new IListener() {
                                            @Override
                                            public void execute() {
                                                Toast.makeText(app, "Download finished.", Toast.LENGTH_SHORT).show();
                                                FileManagerFragmentCloud.this.app.refreshAdapters();
                                            }
                                        });
                                        break;

                                    case 1:
                                        FileManagerFragmentCloud.this.app.prompt("Rename", "Rename " + (modelFile.directory ? "directory" : "file") + " " + modelFile.name + " ?", "Ok", new IStringListener() {
                                            @Override
                                            public void execute(String text) {
                                                modelFile.rename(text, new IPostExecuteListener() {
                                                    @Override
                                                    public void execute(JSONObject json, String body) {
                                                        FileManagerFragmentCloud.this.app.refreshAdapters();
                                                    }
                                                });
                                            }
                                        }, "Cancel", null, modelFile.name);
                                        break;

                                    case 2:
                                        FileManagerFragmentCloud.this.app.alert("Delete", "Delete " + (modelFile.directory ? "directory" : "file") + " " + modelFile.name + " ?", "Yes", new IListener() {
                                            @Override
                                            public void execute() {
                                                modelFile.delete(new IPostExecuteListener() {
                                                    @Override
                                                    public void execute(JSONObject json, String body) {
                                                        FileManagerFragmentCloud.this.app.refreshAdapters();
                                                    }
                                                });
                                            }
                                        }, "No", null);
                                        break;

                                    case 3:
                                        FileManagerFragmentCloud.this.filesToCut = new ArrayList<>();
                                        FileManagerFragmentCloud.this.filesToCut.add(modelFile);
                                        Toast.makeText(app, "File ready to cut.", Toast.LENGTH_SHORT).show();
                                        break;

                                    case 4:
                                        modelFile.setPublic(!modelFile.public_, new IPostExecuteListener() {
                                            @Override
                                            public void execute(JSONObject json, String body) {
                                                FileManagerFragmentCloud.this.app.refreshAdapters();
                                            }
                                        });
                                        break;
                                }
                            }
                        });
                AlertDialog menuDrop = menuAleart.create();
                menuDrop.show();
            }
        });
        this.listView.setAdapter(adapter);
        this.listView.setItemAnimator(/*new SlideInFromLeftItemAnimator(mRecyclerView)*/new DefaultItemAnimator());
        this.listView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        this.adapter.setOnItemClickListener(new AdapterModelFile.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(files.get(position).directory) {
                    FileManagerFragmentCloud.this.url = files.get(position).url + "/";
                    refreshList();
                }
                else
                    files.get(position).executeOnline(files);
            }
        });

        this.adapter.setOnItemLongClickListener(new AdapterModelFile.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int position) {

                return true;
            }
        });

        refreshList();

		return rootView;
	}
	
	public void refreshList() {
		refreshList(null);
	}

	public void refreshList(String search) {
		List<BasicNameValuePair> parameters = new ArrayList<>();
		if(search!=null)
			parameters.add(new BasicNameValuePair("search", ""+search));
        parameters.add(new BasicNameValuePair("url", ""+this.url));
        parameters.add(new BasicNameValuePair("all-public", ""+true));

        if(this.app.isInternetConnection())
            new TaskGet(
                app,
                this.app.getConfig().getUser(),
                this.app.getConfig().getUrlServer() + this.app.getConfig().routeFile,
                new IPostExecuteListener() {
                    @Override
                    public void execute(JSONObject json, String body) {
                        if(!isAdded())
                            return;
                        files = new ArrayList<>();
                        try {
                            if (json != null) {
                                if (json.has("result")) {
                                    JSONArray array = json.getJSONArray("result");
                                    for (int i = 0; i < array.length(); i++) {
                                        ModelFile modelFile = new ModelFile(app, array.getJSONObject(i));
                                        files.add(modelFile);
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
            this.message.setText(getString(R.string.no_internet_connection));
            this.message.setVisibility(View.VISIBLE);
            this.swipeRefreshLayout.setRefreshing(false);
        }
	}

	public void updateAdapter() {
		if(this.listView!=null && this.files!=null && this.isAdded()) {

            this.circularProgressBar.setVisibility(View.GONE);
            if( this.circle.getVisibility()==View.GONE ) {
                this.circle.setVisibility(View.VISIBLE);
                this.circle.startAnimation(animOpen);
            }

			if(this.files.size()==0) {
                if(this.url==null)
				    this.message.setText(getString(R.string.no_file_server));
                else if(this.url.equals(""))
                    this.message.setText(getString(R.string.no_file_server));
                else
                    this.message.setText(getString(R.string.no_file_directory));
				this.message.setVisibility(View.VISIBLE);
			}
			else
				this.message.setVisibility(View.GONE);

            this.adapter.remplaceList(this.files);

            if(this.url==null)
                this.circle2.setVisibility(View.GONE);
            else if(this.url.equals(""))
                this.circle2.setVisibility(View.GONE);
            else
                this.circle2.setVisibility(View.VISIBLE);

            this.swipeRefreshLayout.setRefreshing(false);
		}
	}

    @Override
    public boolean back() {
        return false;
    }
}
