package com.mercandalli.android.apps.files.support;

import com.mercandalli.android.apps.files.main.FileAppComponent;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module used by the {@link FileAppComponent}.
 */
@Module
public class SupportModule {

    @Provides
    @Singleton
    SupportManager provideSupportManager() {
        return new SupportManagerMockImpl();
    }

}