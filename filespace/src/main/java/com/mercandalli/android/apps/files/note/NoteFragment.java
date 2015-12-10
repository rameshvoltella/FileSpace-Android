package com.mercandalli.android.apps.files.note;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mercandalli.android.apps.files.R;
import com.mercandalli.android.apps.files.common.fragment.BackFragment;
import com.mercandalli.android.apps.files.common.listener.IListener;
import com.mercandalli.android.apps.files.common.util.DialogUtils;
import com.mercandalli.android.apps.files.common.util.FontUtils;
import com.mercandalli.android.apps.files.common.util.StringUtils;
import com.mercandalli.android.apps.files.file.filespace.FileSpaceModel;

import java.io.UnsupportedEncodingException;

/**
 * Created by Jonathan on 21/07/2015.
 */
public class NoteFragment extends BackFragment {

    private View rootView;
    private TextView input;

    private FileSpaceModel mFileSpaceModel;

    public static NoteFragment newInstance() {
        return new NoteFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.fragment_workspace_note, container, false);

        mFileSpaceModel = new FileSpaceModel.FileSpaceModelBuilder().type("article").build();

        this.input = (TextView) this.rootView.findViewById(R.id.input);
        FontUtils.applyFont(mActivity, this.input, "fonts/Roboto-Light.ttf");

        String txt = mApplicationCallback.getConfig().getUserNoteWorkspace1();
        if (!StringUtils.isNullOrEmpty(txt)) {
            try {
                String txt_tmp = new String(txt.getBytes("ISO-8859-1"), "UTF-8");
                this.input.setText(txt_tmp);
                mFileSpaceModel.getArticle().article_content_1 = txt_tmp;
            } catch (UnsupportedEncodingException e) {
                Log.e(getClass().getName(), "Failed UnsupportedEncodingException", e);
            }
        }

        this.input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    mApplicationCallback.getConfig().setUserNoteWorkspace1(mActivity, s.toString());
                    mFileSpaceModel.getArticle().article_content_1 = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return this.rootView;
    }

    @Override
    public boolean back() {
        return false;
    }

    @Override
    public void onFocus() {

    }

    public void delete() {
        DialogUtils.alert(mActivity, "Delete note", "Delete the current note?", getString(R.string.yes), new IListener() {
            @Override
            public void execute() {
                input.setText("");
                mApplicationCallback.getConfig().setUserNoteWorkspace1(mActivity, "");
            }
        }, getString(R.string.no), null);
    }
}

