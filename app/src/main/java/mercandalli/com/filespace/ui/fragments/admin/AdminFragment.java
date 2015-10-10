/**
 * This file is part of FileSpace for Android, an app for managing your server (files, talks...).
 *
 * Copyright (c) 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 *
 * LICENSE:
 *
 * FileSpace for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * FileSpace for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * @author Jonathan Mercandalli
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2014-2015 FileSpace for Android contributors (http://mercandalli.com)
 */
package mercandalli.com.filespace.ui.fragments.admin;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mercandalli.com.filespace.R;
import mercandalli.com.filespace.ui.activities.Application;
import mercandalli.com.filespace.ui.activities.ApplicationDrawer;
import mercandalli.com.filespace.ui.fragments.EmptyFragment;
import mercandalli.com.filespace.ui.fragments.BackFragment;
import mercandalli.com.filespace.ui.views.NonSwipeableViewPager;
import mercandalli.com.filespace.ui.views.PagerSlidingTabStrip;


public class AdminFragment extends BackFragment {

    private static final int NB_FRAGMENT = 8;
    private static final int INIT_FRAGMENT = 0;
    public static final BackFragment LIST_BACK_FRAGMENT[] = new BackFragment[NB_FRAGMENT];
    private NonSwipeableViewPager mViewPager;
    private FileManagerFragmentPagerAdapter mPagerAdapter;
    private PagerSlidingTabStrip tabs;

    public AdminFragment() {
        super();
    }

    public void setApp(ApplicationDrawer app) {
        this.app = app;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_admin, container, false);
        mPagerAdapter = new FileManagerFragmentPagerAdapter(this.getChildFragmentManager(), app);

        tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
        mViewPager = (NonSwipeableViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setNonSwipeableItem(7);
        mViewPager.setAdapter(mPagerAdapter);
        tabs.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                AdminFragment.this.app.invalidateOptionsMenu();
                if (position < NB_FRAGMENT)
                    if (LIST_BACK_FRAGMENT[position] != null)
                        LIST_BACK_FRAGMENT[position].onFocus();
            }
        });
        mViewPager.setOffscreenPageLimit(NB_FRAGMENT - 1);
        mViewPager.setCurrentItem(INIT_FRAGMENT);

        tabs.setViewPager(mViewPager);
        tabs.setIndicatorColor(getResources().getColor(R.color.white));

        return rootView;
    }

    public int getCurrentFragmentIndex() {
        if(mViewPager == null)
            return -1;
        int result = mViewPager.getCurrentItem();
        if(result >= LIST_BACK_FRAGMENT.length)
            return -1;
        return mViewPager.getCurrentItem();
    }

    @Override
    public boolean back() {
        int currentFragmentId = getCurrentFragmentIndex();
        if(LIST_BACK_FRAGMENT == null || currentFragmentId== -1)
            return false;
        BackFragment backFragment = LIST_BACK_FRAGMENT[currentFragmentId];
        if(backFragment ==null)
            return false;
        return backFragment.back();
    }

    @Override
    public void onFocus() {

    }

    public class FileManagerFragmentPagerAdapter extends FragmentPagerAdapter {
        Application app;

        public FileManagerFragmentPagerAdapter(FragmentManager fm, Application app) {
            super(fm);
            this.app = app;
        }

        @Override
        public BackFragment getItem(int i) {
            BackFragment backFragment = null;
            switch(i) {
                case 0:		backFragment = ServerDataFragment.newInstance();  	break;
                case 1:		backFragment = ServerLogsFragment.newInstance();    break;
                case 2:		backFragment = UserAddFragment.newInstance(); 	    break;
                case 5:		backFragment = RequestFragment.newInstance();    	break;
                case 6:		backFragment = GameFragment.newInstance();    	    break;
                case 7:		backFragment = StatisticsFragment.newInstance();    break;
                default:    backFragment = EmptyFragment.newInstance();         break;
            }
            LIST_BACK_FRAGMENT[i] = backFragment;
            return backFragment;
        }

        @Override
        public int getCount() {
            return NB_FRAGMENT;
        }

        @Override
        public CharSequence getPageTitle(int i) {
            String title = "null";
            switch(i) {
                case 0:		title = "SERVER DATA";      break;
                case 1:		title = "SERVER LOGS";	    break;
                case 2:		title = "USER ADD";         break;
                case 3:		title = "USER DATA";        break;
                case 4:		title = "USER LICENCES";    break;
                case 5:		title = "REQUEST";          break;
                case 6:		title = "GAME";             break;
                case 7:		title = "STATS";            break;
            }
            return title;
        }
    }
}