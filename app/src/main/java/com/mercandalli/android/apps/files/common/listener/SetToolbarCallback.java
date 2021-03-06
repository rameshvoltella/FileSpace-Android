package com.mercandalli.android.apps.files.common.listener;

import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;

/**
 * Interface used to set the {@link Toolbar} inside {@link android.app.Fragment}.
 */
public interface SetToolbarCallback {

    void setToolbar(Toolbar toolbar);

    void setTitleToolbar(@StringRes int title);
}
