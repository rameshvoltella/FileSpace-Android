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
package com.mercandalli.android.apps.files.common.net;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mercandalli.android.apps.files.R;
import com.mercandalli.android.apps.files.common.listener.IListener;
import com.mercandalli.android.apps.files.file.FileModel;
import com.mercandalli.android.apps.files.file.FileUtils;
import com.mercandalli.android.apps.files.main.Config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Global behavior : DDL file
 *
 * @author Jonathan
 */
public class TaskGetDownload extends AsyncTask<Void, Long, Void> {

    String url;
    String mUrlOuput;
    IListener listener;
    Activity mActivity;

    long mFileSize;
    String mFileTypeTitle;

    int id = 1;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;

    public TaskGetDownload(Activity activity, String url, String mUrlOuput, FileModel fileModel, IListener listener) {
        mActivity = activity;
        this.url = url;
        this.mUrlOuput = mUrlOuput;
        this.mFileSize = fileModel.getSize();
        this.mFileTypeTitle = fileModel.getType().getTitle(activity);
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mNotifyManager = (NotificationManager) this.mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this.mActivity);
        mBuilder.setContentTitle(mFileTypeTitle + " Download")
                .setContentText("Download in progress : 0 / " + FileUtils.humanReadableByteCount(mFileSize) + " : 0%")
                .setSmallIcon(R.drawable.ic_notification_cloud);
    }

    @Override
    protected Void doInBackground(Void... urls) {
        fileFromUrlAuthorization(this.url);
        return null;
    }

    @Override
    protected void onPostExecute(Void response) {
        Log.d("onPostExecute", "" + response);

        // When the loop is finished, updates the notification
        mBuilder.setContentText("Download complete")
                // Removes the progress bar
                .setProgress(0, 0, false);
        mNotifyManager.notify(id, mBuilder.build());

        this.listener.execute();
    }

    public void fileFromUrlAuthorization(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setRequestProperty("Authorization", "Basic " + Config.getUserToken());
            conn.setRequestMethod("GET");

            InputStream inputStream = conn.getInputStream();
            long lengthOfFile = Long.parseLong(conn.getHeaderField("Content-Length"));
            OutputStream outputStream = new FileOutputStream(mUrlOuput);

            byte data[] = new byte[1024];
            long total = 0;
            int missed_value = 50;
            int missed_counter = 0;

            int count;
            while ((count = inputStream.read(data)) != -1) {
                total += count;

                missed_counter++;
                if (missed_counter > missed_value) {
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress(((total * 100) / lengthOfFile), total);

                    missed_counter = 0;
                }

                // writing data to file
                outputStream.write(data, 0, count);
            }

            // flushing output
            outputStream.flush();

            // closing streams
            outputStream.close();
            inputStream.close();

            conn.disconnect();

        } catch (IOException e) {
            Log.e(getClass().getName(), "IOException: Download exception.", e);
        }
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);

        long incr = 0;
        if (values.length > 0) {
            incr = values[0];
        }
        mBuilder.setProgress(100, (int) incr, false);
        mBuilder.setContentText("Download in progress " + incr + "%");
        if (values.length > 1) {
            mBuilder.setContentText("Download in progress : " + FileUtils.humanReadableByteCount(values[1]) + " / " + FileUtils.humanReadableByteCount(mFileSize) + " : " + incr + "%");
        }

        mNotifyManager.notify(id, mBuilder.build());
    }
}
