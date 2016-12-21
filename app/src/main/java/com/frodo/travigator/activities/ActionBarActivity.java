package com.frodo.travigator.activities;

/**
 * Created by Kapil on 9/6/2015.
 */

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;


import com.frodo.travigator.R;
import com.frodo.travigator.adapter.TabsPagerAdapter;
import com.frodo.travigator.app.trApp;
import com.frodo.travigator.fragments.HomeFragment;
import com.frodo.travigator.utils.CommonUtils;
import com.frodo.travigator.utils.LocationUtil;
import com.frodo.travigator.utils.PrefManager;

public class ActionBarActivity extends FragmentActivity implements
        ActionBar.TabListener {

    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    // Tab titles
    private String[] tabs = {"Favorites", "Search"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //    setContentView(R.layout.actionbaractivity);

        // Initilization
        //    viewPager = (ViewPager) findViewById(R.id.pager);

        viewPager = new ViewPager(this);
        viewPager.setId(R.id.pager);

        setContentView(viewPager);

        actionBar = getActionBar();
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
        for (String tab_name : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }

        SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.appModePref), Context.MODE_PRIVATE);
        int appMode = sp.getInt(getString(R.string.appModePref), 0);

        if ( appMode > 0 ) {
            PrefManager.enableTTS(true);
            trApp.getTTS().speak(getString(R.string.easyWelcome), TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            PrefManager.enableTTS(false);
        }

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        // on tab selected
        // show respected fragment view
        viewPager.setCurrentItem(tab.getPosition());
        if (tab.getPosition() == 1) {
            HomeFragment homeFragment = (HomeFragment)getSupportFragmentManager()
                    .findFragmentByTag("android:switcher:"+viewPager.getId()+":"+tab.getPosition());
            homeFragment.init();
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LocationUtil.REQ_PERMISSIONS_REQUEST_ACCESS_FILE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    trApp.getLocationUtil().checkLocationSettings(this);
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LocationUtil.REQUEST_CHECK_SETTINGS:
                trApp.getLocationUtil().dialogClosed();
        }
    }
}