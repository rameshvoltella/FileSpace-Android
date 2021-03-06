package com.mercandalli.android.apps.files.file.cloud.response;

import android.content.Context;

import com.mercandalli.android.apps.files.main.network.MyResponse;

import java.util.List;

public class FilesResponse extends MyResponse<FileResponse> {

    @Override
    public List<FileResponse> getResult(final Context context) {
        return super.getResult(context);
    }

    public String getToast() {
        return mToast;
    }
}
