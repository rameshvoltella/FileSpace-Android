package com.mercandalli.android.apps.files.file.audio;

import android.content.Context;
import android.util.Log;

import com.mercandalli.android.apps.files.common.listener.IPostExecuteListener;
import com.mercandalli.android.apps.files.common.net.TaskGet;
import com.mercandalli.android.apps.files.common.util.StringPair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The file audio cover util.
 */
public class CoverUtils {

    /**
     * Get the cover from an album.
     */
    public static void getCoverUrl(final Context context, final FileAudioModel fileAudioModel, final CoverResponse resultUrl) {
        final String albumSearch = fileAudioModel.getName().replaceAll("\\s", "+");

        final List<StringPair> parameters = new ArrayList<>();
        parameters.add(new StringPair("term", albumSearch));

        new TaskGet(context, "https://itunes.apple.com/search", new IPostExecuteListener() {
            @Override
            public void onPostExecute(JSONObject json, String body) {
                if (json != null) {
                    try {
                        if (json.has("results")) {
                            JSONArray res = json.getJSONArray("results");
                            if (res.length() >= 1) {
                                JSONObject track = res.getJSONObject(0);
                                if (track.has("artworkUrl100")) {
                                    resultUrl.onCoverUrlResult(fileAudioModel, track.getString("artworkUrl100"));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(getClass().getName(), "Failed to convert Json", e);
                    }
                }
            }
        }, parameters, false).execute();
    }

    public interface CoverResponse {
        void onCoverUrlResult(FileAudioModel fileAudioModel, String url);
    }
}
