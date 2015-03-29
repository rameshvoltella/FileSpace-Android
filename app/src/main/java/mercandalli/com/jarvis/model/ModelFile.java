/**
 * Personal Project : Control server
 *
 * MERCANDALLI Jonathan
 */

package mercandalli.com.jarvis.model;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import mercandalli.com.jarvis.R;
import mercandalli.com.jarvis.activity.ActivityFileAudio;
import mercandalli.com.jarvis.activity.ActivityFileText;
import mercandalli.com.jarvis.activity.Application;
import mercandalli.com.jarvis.listener.IBitmapListener;
import mercandalli.com.jarvis.listener.IListener;
import mercandalli.com.jarvis.listener.IPostExecuteListener;
import mercandalli.com.jarvis.net.TaskDelete;
import mercandalli.com.jarvis.net.TaskGetDownload;
import mercandalli.com.jarvis.net.TaskGetDownloadImage;
import mercandalli.com.jarvis.net.TaskPost;

public class ModelFile extends Model implements Parcelable {
	
	public int id, id_user;
	public String url;
	public String name;
	public long size;
	public ModelFileType type;
	public boolean directory = false;
    public boolean public_ = false;
	public Bitmap bitmap;
	public File file;
    public String onlineUrl;
    public ModelFileContent content;

    public CountDownTimer cdt;

    public String getAdapterTitle() {
        if(this.type.toString().equals("jarvis") && this.content != null)
            return this.content.toString();
        else if(this.name!=null)
            return this.getNameExt();
        else
            return this.url;
    }

    public String getAdapterSubtitle() {
        if(this.directory)
            return "Directory";
        if(this.type.toString().equals("jarvis") && this.content != null)
            return type.getTitle() + " " + this.content.type;
        return type.getTitle();
    }

    public String getNameExt() {
        return this.name + ((this.directory) ? "" : ("." + this.type));
    }

	public List<BasicNameValuePair> getForUpload() {
		List<BasicNameValuePair> parameters = new ArrayList<>();
		if(name!=null)
			parameters.add(new BasicNameValuePair("url", this.name));
        if(directory)
            parameters.add(new BasicNameValuePair("directory", this.directory?"true":"false"));
		return parameters;
	}
	
	public ModelFile(Application app) {
		super(app);
	}
	
	public ModelFile(Application app, JSONObject json) {
		super(app);
		
		try {
			if(json.has("id") && !json.isNull("id")) {
                this.id = json.getInt("id");
                this.onlineUrl = this.app.getConfig().getUrlServer()+this.app.getConfig().routeFile+"/"+id;
            }
            if(json.has("id_user") && !json.isNull("id_user")) {
                this.id_user = json.getInt("id_user");
            }
			if(json.has("url"))
                this.url = json.getString("url");
            if(json.has("name"))
                this.name = json.getString("name");
			if(json.has("type"))
                this.type = new ModelFileType(json.getString("type"));
            if(json.has("size") && !json.isNull("size"))
                this.size = json.getLong("size");
            if(json.has("directory") && !json.isNull("directory"))
                this.directory = json.getInt("directory")==1;
            if(json.has("content") && !json.isNull("content"))
                this.content = new ModelFileContent(json.getString("content"));
            if(json.has("public") && !json.isNull("public"))
                this.public_ = json.getInt("public")==1;

		} catch (JSONException e) {
            Log.e("model ModelFile", "JSONException");
			e.printStackTrace();
		}
		
		if(this.type.equals(ModelFileTypeENUM.PICTURE.type) && this.size >= 0 && this.size < 100000) {
			new TaskGetDownloadImage(app, this.app.getConfig().getUser(), this, new IBitmapListener() {
				@Override
				public void execute(Bitmap bitmap) {
					ModelFile.this.bitmap = bitmap;
					ModelFile.this.app.updateAdapters();
				}
			}).execute();
		}		
	}
	
	public void executeOnline(ArrayList<ModelFile> files) {
		if(this.type.equals(ModelFileTypeENUM.TEXT.type)) {
            Intent intent = new Intent(this.app, ActivityFileText.class);
            intent.putExtra("URL_FILE", ""+this.onlineUrl);
            intent.putExtra("LOGIN", ""+this.app.getConfig().getUser().getAccessLogin());
            intent.putExtra("PASSWORD", ""+this.app.getConfig().getUser().getAccessPassword());
            intent.putExtra("ONLINE", true);
            this.app.startActivity(intent);
            this.app.overridePendingTransition(R.anim.left_in, R.anim.left_out);
		}
		else if(this.type.equals(ModelFileTypeENUM.AUDIO.type)) {
            Intent intent = new Intent(app, ActivityFileAudio.class);
            intent.putExtra("LOGIN", ""+app.getConfig().getUser().getAccessLogin());
            intent.putExtra("PASSWORD", ""+app.getConfig().getUser().getAccessPassword());
            intent.putExtra("ONLINE", true);
            intent.putExtra("FILE", this);
            ArrayList<ModelFile> tmpFiles = new ArrayList<ModelFile>();
            for(ModelFile f:files)
                if(f.type.equals(ModelFileTypeENUM.AUDIO.type))
                    tmpFiles.add(f);
            intent.putParcelableArrayListExtra("FILES", tmpFiles);
            this.app.startActivity(intent);
            this.app.overridePendingTransition(R.anim.left_in, R.anim.left_out);
		}
	}
	
	public void executeLocal(ArrayList<ModelFile> files) {
		if (!file.exists())
			return;
		if (this.type.equals(ModelFileTypeENUM.APK.type)) {
			Intent apkIntent = new Intent();
			apkIntent.setAction(Intent.ACTION_VIEW);
			apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            this.app.startActivity(apkIntent);
		}
		else if(this.type.equals(ModelFileTypeENUM.TEXT.type)) {
			Intent txtIntent = new Intent();
			txtIntent.setAction(Intent.ACTION_VIEW);
			txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");
			try {
				this.app.startActivity(txtIntent);
			} catch (ActivityNotFoundException e) {
				txtIntent.setType("text/*");
                this.app.startActivity(txtIntent);
			}
		}
		else if(this.type.equals(ModelFileTypeENUM.HTML.type)) {
			Intent htmlIntent = new Intent();
			htmlIntent.setAction(Intent.ACTION_VIEW);
			htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");
			try {
				this.app.startActivity(htmlIntent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(this.app, "ERREUR", Toast.LENGTH_SHORT).show();
			}
		}
		else if(this.type.equals(ModelFileTypeENUM.AUDIO.type)) {
            Intent intent = new Intent(this.app, ActivityFileAudio.class);
            intent.putExtra("ONLINE", false);
            intent.putExtra("FILE", this);
            ArrayList<ModelFile> tmpFiles = new ArrayList<ModelFile>();
            for(ModelFile f:files)
                if(f.type!=null)
                    if(f.type.equals(ModelFileTypeENUM.AUDIO.type))
                        tmpFiles.add(f);
            intent.putParcelableArrayListExtra("FILES", tmpFiles);
            this.app.startActivity(intent);
            this.app.overridePendingTransition(R.anim.left_in, R.anim.left_out);
		}
		else if(this.type.equals(ModelFileTypeENUM.PICTURE.type)) {
			Intent picIntent = new Intent();
			picIntent.setAction(Intent.ACTION_VIEW);
			picIntent.setDataAndType(Uri.fromFile(file), "image/*");
            this.app.startActivity(picIntent);
		}
		else if(this.type.equals(ModelFileTypeENUM.VIDEO.type)) {
			Intent movieIntent = new Intent();
			movieIntent.setAction(Intent.ACTION_VIEW);
			movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
			app.startActivity(movieIntent);
		}
		else if(this.type.equals(ModelFileTypeENUM.PDF.type)) {
			Intent pdfIntent = new Intent();
			pdfIntent.setAction(Intent.ACTION_VIEW);
			pdfIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
			try {
				app.startActivity(pdfIntent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(app, "ERREUR", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void download(IListener listener) {
        if(this.directory) {
            Toast.makeText(app, "Directory download not supported yet.", Toast.LENGTH_SHORT).show();
            return;
        }
		String url = this.app.getConfig().getUrlServer()+this.app.getConfig().routeFile+"/"+id;
		String url_ouput = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+app.getConfig().localFolderName+File.separator+this.getNameExt();
		new TaskGetDownload(this.app, url, url_ouput, this, listener).execute();
	}
	
	public boolean isOnline() {
		return (file==null);
	}
	
	public void delete(IPostExecuteListener listener) {
		if(this.isOnline()) {
			String url = this.app.getConfig().getUrlServer()+this.app.getConfig().routeFile+"/"+id;
			new TaskDelete(app, url, listener).execute();
		}
		else {
			file.delete();
            if(listener!=null)
			    listener.execute(null, null);
		}
	}

    public void setPublic(boolean public_, IPostExecuteListener listener) {
        this.public_ = public_;

        List<BasicNameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("public", "" + this.public_));
        String url = this.app.getConfig().getUrlServer() + this.app.getConfig().routeFile + "/" + this.id + "?test=coucou";
        (new TaskPost(this.app, url, listener, parameters)).execute();
    }
	
	public void rename(String new_name, IPostExecuteListener listener) {
		this.name = new_name;
		this.url = new_name;
		String url = this.app.getConfig().getUrlServer()+this.app.getConfig().routeFile+"/"+id;
		new TaskPost(app, url, listener, getForUpload()).execute();
	}

    private void copyFile(String outputPath, IPostExecuteListener listener) {
        if(this.isOnline()) {
            //TODO
        }
        else {
            InputStream in = null;
            OutputStream out = null;
            try {
                File dir = new File (outputPath);
                if (!dir.exists())
                    dir.mkdirs();

                in = new FileInputStream(this.file.getAbsoluteFile());
                out = new FileOutputStream(outputPath + this.getNameExt());

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            }
            catch (FileNotFoundException e) {
                Log.e("tag", e.getMessage());
            }
            catch (Exception e) {
                Log.e("tag", e.getMessage());
            }
        }
        if(listener!=null)
            listener.execute(null, null);
    }

    private void moveFile(String outputPath, IPostExecuteListener listener) {
        if(this.isOnline()) {
            //TODO
        }
        else {
            copyFile(outputPath, null);
            this.delete(null);
        }
        if(listener!=null)
            listener.execute(null, null);
    }

    public boolean isMine() {
        return this.id_user == this.app.getConfig().getUser().id;
    }

    public static final Parcelable.Creator<ModelFile> CREATOR = new Parcelable.Creator<ModelFile>() {
        @Override
        public ModelFile createFromParcel(Parcel source) {
            return new ModelFile(source);
        }
        @Override
        public ModelFile[] newArray(int size) {
            return new ModelFile[size];
        }
    };

    public ModelFile(Parcel in) {
        this.id = in.readInt();
        this.url = in.readString();
        this.onlineUrl = in.readString();
        this.name = in.readString();
        this.size = in.readLong();
        boolean[] b = new boolean[1];
        in.readBooleanArray(b);
        this.directory = b[0];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.url);
        dest.writeString(this.onlineUrl);
        dest.writeString(this.name);
        dest.writeLong(this.size);
        dest.writeBooleanArray(new boolean[]{this.directory});
    }
}
