package com.frodo.travigator.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.frodo.travigator.R;
import com.frodo.travigator.activities.NavigateActivity;
import com.frodo.travigator.app.trApp;
import com.frodo.travigator.db.DbHelper;
import com.frodo.travigator.events.DBChangedEvent;
import com.frodo.travigator.models.Stop;
import com.frodo.travigator.utils.CommonUtils;
import com.frodo.travigator.utils.Constants;
import com.frodo.travigator.utils.PrefManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class Favorite extends Fragment {
    private static final String CITY_LIST = "city_list", ROUTE_LIST="route_list", STOPS_LIST="stops_list";
    private ArrayList<String> cityList, routeList;
    public ArrayList<String> stopList;
    public String City = "", Route = "";

    private static boolean easyMode = false;

    private int destPos, srcPos;
    private Stop[] stops;

    private Spinner citySpinner, routeSpinner, stopSpinner, srcStopSpinner;
    private TextView favText;
    private Button prevChoice, navigate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        SharedPreferences appMode = getContext().getSharedPreferences(getString(R.string.appModePref), Context.MODE_PRIVATE);
        if (appMode.getInt(getString(R.string.appModePref), 0) > 0 ) easyMode = true;
        else easyMode = false;

        View rootView;
        if ( easyMode ) {
            rootView = inflater.inflate(R.layout.favorite_easy, container, false);
        }
        else {
            rootView = inflater.inflate(R.layout.favorite, container, false);
        }

        if (savedInstanceState != null) {
            cityList = savedInstanceState.getStringArrayList(CITY_LIST);
            routeList = savedInstanceState.getStringArrayList(ROUTE_LIST);
            stops = (Stop[])savedInstanceState.getSerializable(STOPS_LIST);
            stopList = (ArrayList<String>) CommonUtils.getStringArray(stops);
            stopList = savedInstanceState.getStringArrayList("stopList");
        } else {
            stopList = new ArrayList<String>();
            routeList = new ArrayList<String>();
            cityList = new ArrayList<String>();

            stopList.add(getString(R.string.routeFirst));
            routeList.add(getString(R.string.cityFirst));
            cityList.add(getString(R.string.selectCity));

            File dir = new File(DbHelper.DATABASE_PATH);
            File files[] = dir.listFiles();
            int l = 0;
            if (files != null) l = files.length;

            for (int i = 0; i < l; i++) {
                String name = files[i].getName();
                name = CommonUtils.capitalize(name);
                if (name.contains("journal") == false) {
                    cityList.add(name);
                }
            }
        }

        citySpinner = (Spinner) rootView.findViewById(R.id.cityFav);
        routeSpinner = (Spinner) rootView.findViewById(R.id.routeFav);
        stopSpinner = (Spinner) rootView.findViewById(R.id.stopFav);
        srcStopSpinner = (Spinner) rootView.findViewById(R.id.srcStopFav);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setOnItemSelectedListener(cityListener);

        if (cityList.size() == 2) {
            citySpinner.setSelection(1);
        }
        if (cityList.size() == 1) {
            cityList.remove(0);
            cityList.add(getString(R.string.favNotFound));
        }

        ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeSpinner.setAdapter(routeAdapter);
        routeSpinner.setOnItemSelectedListener(routeListener);

        ArrayAdapter<String> stopAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final ArrayAdapter<String> srcStopAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
        srcStopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stopSpinner.setAdapter(stopAdapter);
        stopSpinner.setOnItemSelectedListener(stopListener);
        srcStopSpinner.setAdapter(srcStopAdapter);
        srcStopSpinner.setOnItemSelectedListener(srcStopListener);

        navigate = (Button) rootView.findViewById(R.id.buttonNavigate);
        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (City == "") {
                    Toast.makeText(getActivity(), getString(R.string.selectCity), Toast.LENGTH_SHORT).show();
                } else if (Route == "") {
                    Toast.makeText(getActivity(), getString(R.string.selectRoute), Toast.LENGTH_SHORT).show();
                } else {
                    if (stops == null) {
                        CommonUtils.toast("Please select route you want to navigate");
                        return;
                    } else if(srcPos == -1 || destPos == -1) {
                        CommonUtils.toast("Please select source and destination");
                        return;
                    }
                    if ( srcPos == destPos ) {
                        CommonUtils.toast(getString(R.string.sameSrcDest));
                    }
                    Intent navitaionActivity = new Intent(getActivity(), NavigateActivity.class);
                    navitaionActivity.putExtra(NavigateActivity.STOPS, stops);
                    navitaionActivity.putExtra(NavigateActivity.SRC_STOP, srcPos);
                    navitaionActivity.putExtra(NavigateActivity.DST_STOP, destPos);
                    startActivity(navitaionActivity);
                }
            }

        });


        if ( easyMode ) {
            favText = (TextView) rootView.findViewById(R.id.favText);
            favText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( citySpinner.getVisibility() == View.VISIBLE ) citySpinner.performClick();
                    else if ( routeSpinner.getVisibility() == View.VISIBLE ) routeSpinner.performClick();
                    else if ( srcStopSpinner.getVisibility() == View.VISIBLE ) srcStopSpinner.performClick();
                    else if ( stopSpinner.getVisibility() == View.VISIBLE ) stopSpinner.performClick();
                }
            });

            prevChoice = (Button) rootView.findViewById(R.id.buttonPrevChoice);
            prevChoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( routeSpinner.getVisibility() == View.VISIBLE ) {
                        favText.setText(R.string.easySelectCity);
                        routeSpinner.setVisibility(View.GONE);
                        citySpinner.setVisibility(View.VISIBLE);
                        prevChoice.setEnabled(false);
                    }
                    else if ( srcStopSpinner.getVisibility() == View.VISIBLE ) {
                        favText.setText(R.string.easySelectRoute);
                        srcStopSpinner.setVisibility(View.GONE);
                        routeSpinner.setVisibility(View.VISIBLE);
                    }
                    else if ( stopSpinner.getVisibility() == View.VISIBLE ) {
                        favText.setText(R.string.easySelectSrc);
                        stopSpinner.setVisibility(View.GONE);
                        srcStopSpinner.setVisibility(View.VISIBLE);
                        navigate.setEnabled(false);
                    }
                }
            });
        }

        Button remFav = (Button) rootView.findViewById(R.id.buttonRemFav);
        remFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Route != "") {
                    DbHelper db = new DbHelper(getActivity(), City, "route_"+CommonUtils.deCapitalize(Route));
                    db.delTable();
                    db.closeDB();
                    citySpinner.setSelection(0);
                } else if (City == "") {
                    Toast.makeText(getActivity(), getString(R.string.selectCity), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.selectRoute), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.favorite);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        //MenuInflater inflater = getMenuInflater()
        inflater.inflate(R.menu.main_activity_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                CommonUtils.openSettings(getContext());
                return true;
            case R.id.actionSwitchMode:
                SharedPreferences sp = getContext().getSharedPreferences(getString(R.string.appModePref),Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                if (easyMode) {
                    edit.putInt(getString(R.string.appModePref), 0);
                    PrefManager.enableTTS(false);
                }
                else {
                    edit.putInt(getString(R.string.appModePref), 1);
                    PrefManager.enableTTS(true);
                }
                edit.commit();

                CommonUtils.toast("Restarting activity...");
                Intent i = new Intent(getContext(), com.frodo.travigator.activities.ActionBarActivity.class);
                getActivity().finish();
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outstate) {
        super.onSaveInstanceState(outstate);

        outstate.putStringArrayList("cityList", cityList);
        outstate.putStringArrayList("routeList", routeList);
        outstate.putStringArrayList("stopList", stopList);
    }

    private OnItemSelectedListener cityListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            routeList.clear();
            routeList.trimToSize();

            if (pos == 0) {
                routeList.add(getString(R.string.cityFirst));
                City = "";
                routeSpinner.setEnabled(false);
            } else {
                routeList.add(getString(R.string.selectRoute));

                City = CommonUtils.deCapitalize(cityList.get(pos));

                DbHelper db = new DbHelper(trApp.getAppContext(), City);
                Cursor c = db.getTables();

                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    while (!c.isAfterLast()) {
                        String temp = c.getString(0);
                        if (!temp.contains("route_")) {
                            c.moveToNext();
                            continue;
                        }
                        routeList.add(CommonUtils.capitalize(temp.replace("route_","")));
                        c.moveToNext();
                    }
                }
                db.closeDB();

                if ( easyMode ) {
                    if ( PrefManager.isTTSEnabled()) {
                        trApp.getTTS().speak(getString(R.string.easyGuideRoute), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    favText.setText(R.string.easySelectRoute);
                    citySpinner.setVisibility(View.GONE);
                    routeSpinner.setVisibility(View.VISIBLE);
                    prevChoice.setEnabled(true);
                }
                routeSpinner.setEnabled(true);
            }

            Collections.sort(routeList.subList(1, routeList.size()));

            ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeList);
            routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeSpinner.setAdapter(routeAdapter);

            if (routeList.size() == 2) {
                routeSpinner.setSelection(1);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            routeList.clear();
            routeList.trimToSize();

            routeList.add(getString(R.string.cityFirst));

            City = "";

            routeSpinner.setEnabled(false);
            ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeList);
            routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeSpinner.setAdapter(routeAdapter);

        }
    };

    private OnItemSelectedListener routeListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            stopList.clear();
            stopList.trimToSize();

            Route = "";

            if (pos != 0) {
                Route = routeList.get(pos).trim();

                DbHelper db = new DbHelper(trApp.getAppContext(), City, "route_"+CommonUtils.deCapitalize(Route));
                Cursor c = db.showTable();

                stops = new Stop[c.getCount()];
                if (c != null && c.getCount() > 0) {
                    c.moveToFirst();
                    int i = 0;
                    while (!c.isAfterLast()) {
                        stops[i] = new Stop(c.getString(1), String.valueOf(c.getInt(0)),
                                c.getString(2), c.getString(3));
                        i++;
                        c.moveToNext();
                    }
                    stopList = (ArrayList)CommonUtils.getStringArray(stops);
                }
                db.closeDB();

                stopList.add(0,getString(R.string.selectStop));

                if (easyMode) {
                    if (PrefManager.isTTSEnabled()) {
                        trApp.getTTS().speak(getString(R.string.easyGuideSrc), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    favText.setText(R.string.easySelectSrc);
                    routeSpinner.setVisibility(View.GONE);
                    srcStopSpinner.setVisibility(View.VISIBLE);
                }
                stopSpinner.setEnabled(true);
                srcStopSpinner.setEnabled(true);
            } else {
                stopList.add(getString(R.string.routeFirst));
                stopSpinner.setEnabled(false);
                srcStopSpinner.setEnabled(false);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stopSpinner.setAdapter(adapter);
            ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
            srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            srcStopSpinner.setAdapter(srcAdapter);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Route = "";

            stopList.clear();
            stopList.trimToSize();

            stopList.add(getString(R.string.routeFirst));

            stopSpinner.setEnabled(false);
            srcStopSpinner.setEnabled(false);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stopSpinner.setAdapter(adapter);
            ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
            srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            srcStopSpinner.setAdapter(srcAdapter);
        }
    };

    private OnItemSelectedListener srcStopListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            srcPos = position-1;

            if (easyMode && position > 0) {
                if ( PrefManager.isTTSEnabled()) {
                    trApp.getTTS().speak(getString(R.string.easyGuideDest),TextToSpeech.QUEUE_FLUSH, null);
                }
                favText.setText(R.string.easySelectDest);
                srcStopSpinner.setVisibility(View.GONE);
                stopSpinner.setVisibility(View.VISIBLE);
                navigate.setEnabled(true);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            srcPos = -1;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onDBChangedEvent(DBChangedEvent event) {
        stopList = new ArrayList<String>();
        routeList = new ArrayList<String>();
        cityList = new ArrayList<String>();

        stopList.add(getString(R.string.routeFirst));
        routeList.add(getString(R.string.cityFirst));
        cityList.add(getString(R.string.selectCity));

        File dir = new File(DbHelper.DATABASE_PATH);
        File files[] = dir.listFiles();
        int l = 0;
        if (files != null) l = files.length;

        for (int i = 0; i < l; i++) {
            String name = files[i].getName();
            name = CommonUtils.capitalize(name);
            if (name.contains("journal") == false) {
                cityList.add(name);
            }
        }

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setOnItemSelectedListener(cityListener);

        if (cityList.size() == 2) {
            citySpinner.setSelection(1);
        }

        ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeSpinner.setAdapter(routeAdapter);
        routeSpinner.setOnItemSelectedListener(routeListener);

        ArrayAdapter<String> stopAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<String> srcStopAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
        srcStopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stopSpinner.setAdapter(stopAdapter);
        stopSpinner.setOnItemSelectedListener(stopListener);
        srcStopSpinner.setAdapter(srcStopAdapter);
        srcStopSpinner.setOnItemSelectedListener(srcStopListener);
    }

    private OnItemSelectedListener stopListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            destPos = pos-1;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            destPos = -1;
        }
    };


}