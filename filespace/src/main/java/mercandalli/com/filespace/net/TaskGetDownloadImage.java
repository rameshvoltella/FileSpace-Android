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
package mercandalli.com.filespace.net;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import mercandalli.com.filespace.listeners.IBitmapListener;
import mercandalli.com.filespace.listeners.ILongListener;
import mercandalli.com.filespace.models.ModelFile;
import mercandalli.com.filespace.models.ModelUser;
import mercandalli.com.filespace.ui.activities.ApplicationActivity;

import static mercandalli.com.filespace.utils.ImageUtils.is_image;
import static mercandalli.com.filespace.utils.ImageUtils.load_image;
import static mercandalli.com.filespace.utils.ImageUtils.save_image;

/**
 * Global behavior : DDL Image
 *
 * @author Jonathan
 *
 */
public class TaskGetDownloadImage extends AsyncTask<Void, Long, Void> {

    String url;
    Bitmap bitmap;
    IBitmapListener listener;
    ApplicationActivity app;
    private String login, password;
    int idFile;
    long sizeLimit, sizeFile;
    ILongListener progressListener;
    private boolean isAuthentication = true;
    private boolean isModelFile = true;

    public TaskGetDownloadImage(ApplicationActivity app, String url, long sizeLimit, IBitmapListener listener, boolean isAuthentication, boolean isModelFile) {
        this.app = app;
        this.url = url;
        this.listener = listener;
        this.sizeLimit = sizeLimit;
        this.isAuthentication = isAuthentication;
        this.isModelFile = isModelFile;
    }

    public TaskGetDownloadImage(ApplicationActivity app, ModelUser user, ModelFile fileModel, long sizeLimit, IBitmapListener listener) {
        this.app = app;
        this.login = user.getAccessLogin();
        this.password = user.getAccessPassword();
        this.url = fileModel.onlineUrl;
        this.idFile = fileModel.id;
        this.sizeFile = fileModel.size;
        this.listener = listener;
        this.sizeLimit = sizeLimit;
    }

    public TaskGetDownloadImage(ApplicationActivity app, String login, String password, String onlineUrl, int idFile, long sizeFile, long sizeLimit, IBitmapListener listener) {
        this.app = app;
        this.login = login;
        this.password = password;
        this.url = onlineUrl;
        this.idFile = idFile;
        this.sizeFile = sizeFile;
        this.listener = listener;
        this.sizeLimit = sizeLimit;
    }

    public TaskGetDownloadImage(ApplicationActivity app, String login, String password, String onlineUrl, int idFile, long sizeFile, long sizeLimit, IBitmapListener listener, ILongListener progressListener) {
        this.app = app;
        this.login = login;
        this.password = password;
        this.url = onlineUrl;
        this.idFile = idFile;
        this.sizeFile = sizeFile;
        this.listener = listener;
        this.sizeLimit = sizeLimit;
        this.progressListener = progressListener;
    }

    @Override
    protected Void doInBackground(Void... urls) {
        bitmap = drawable_from_url_Authorization();
        return null;
    }

    @Override
    protected void onPostExecute(Void response) {
        this.listener.execute(bitmap);
    }

    public Bitmap drawable_from_url_Authorization() {
        Log.d("TaskGetDownloadImage", "id:" + idFile + "  url:" + url);

        if (isModelFile)
            if (is_image(this.app, this.idFile))
                return load_image(this.app, this.idFile);
        if (this.sizeLimit > 0)
            if (this.sizeLimit < this.sizeFile)
                return null;

        Bitmap x = null;
        try {
            StringBuilder authentication = new StringBuilder().append(app.getConfig().getUser().getAccessLogin()).append(":").append(app.getConfig().getUser().getAccessPassword());
            String result = Base64.encodeBytes(authentication.toString().getBytes());

            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            if (isAuthentication)
                conn.setRequestProperty("Authorization", "Basic " + result);
            conn.setRequestMethod("GET");

            InputStream inputStream = conn.getInputStream();

            String contentLength = conn.getHeaderField("Content-Length");
            if (contentLength == null)
                return x;

            long lengthOfFile = Long.parseLong(contentLength);

            // Get the source image's dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inScaled = false;
            if (this.sizeFile > 3000000)
                options.inSampleSize = 16;
            else if (this.sizeFile > 2000000)
                options.inSampleSize = 8;
            else if (this.sizeFile > 500000)
                options.inSampleSize = 4;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            x = BitmapFactory.decodeStream(new FlushedInputStream(inputStream, lengthOfFile), null, options);

            conn.disconnect();

            if (isModelFile)
                save_image(this.app, this.idFile, x);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return x;
    }

    /**
     * DDL image
     * @author Jonathan
     *
     */
    public class FlushedInputStream extends FilterInputStream {

        private long counter = 0, lenghtOfFile;

        public FlushedInputStream(InputStream inputStream, long lenghtOfFile) {
            super(inputStream);
            this.lenghtOfFile = lenghtOfFile;
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int bytet = read();
                    if (bytet < 0)
                        break;  // we reached EOF
                    else
                        bytesSkipped = 1; // we read one byte
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException {
            int result = super.read(buffer, offset, count);
            counter += result;
            if (progressListener != null)
                publishProgress((long) (((counter * 1.0) / lenghtOfFile) * 100));
            return result;
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        if (progressListener != null && values.length > 0)
            progressListener.execute(values[0]);
    }
}