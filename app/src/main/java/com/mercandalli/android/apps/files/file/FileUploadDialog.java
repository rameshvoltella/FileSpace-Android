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
package com.mercandalli.android.apps.files.file;

import android.app.Activity;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mercandalli.android.apps.files.R;
import com.mercandalli.android.apps.files.common.listener.IListener;

public class FileUploadDialog extends Dialog {

    private FileManager mFileManager;

    @NonNull
    private final Activity mActivity;
    private FileChooserDialog mFileChooserDialog;
    private FileModel mFileModel;
    private int mIdFileParent;

    public FileUploadDialog(
            final Activity activity,
            final int id_file_parent,
            final FileModel fileModel,
            final @Nullable IListener listener) {
        this(activity, id_file_parent, listener);

        fileModel.setIdFileParent(id_file_parent);
        ((TextView) FileUploadDialog.this.findViewById(R.id.label)).setText(fileModel.getUrl());
        mFileModel = fileModel;
    }

    public FileUploadDialog(
            @NonNull final Activity activity,
            final int idFileParent,
            @Nullable final IListener listener) {
        super(activity);

        mFileManager = FileManager.getInstance(activity);

        mActivity = activity;
        mIdFileParent = idFileParent;

        setContentView(R.layout.dialog_upload);
        setTitle(R.string.app_name);
        setCancelable(true);

        findViewById(R.id.request).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileModel != null && !mFileModel.isDirectory()) {
                    mFileManager.upload(mFileModel, idFileParent, listener);
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.no_file), Toast.LENGTH_SHORT).show();
                }

                FileUploadDialog.this.dismiss();
            }
        });

        findViewById(R.id.fileButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFileChooserDialog = new FileChooserDialog(mActivity, new FileChooserDialog.FileChooserDialogSelection() {
                    @Override
                    public void onFileChooserDialogSelected(FileModel fileModel, final View view) {
                        fileModel.setIdFileParent(idFileParent);
                        ((TextView) FileUploadDialog.this.findViewById(R.id.label)).setText(fileModel.getUrl());
                        FileUploadDialog.this.mFileModel = fileModel;
                    }
                });
            }
        });

        show();
    }
}
