package com.frodo.travigator.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.frodo.travigator.R;
import com.frodo.travigator.activities.NavigateActivity;
import com.frodo.travigator.app.trApp;
import com.frodo.travigator.db.DbHelper;
import com.frodo.travigator.events.LocationChangedEvent;
import com.frodo.travigator.models.Stop;
import com.frodo.travigator.utils.CommonUtils;
import com.frodo.travigator.utils.Constants;
import com.frodo.travigator.utils.LocationUtil;
import com.frodo.travigator.utils.PrefManager;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String CITY_LIST = "city_list";
    private static final String ROUTE_LIST = "route_list";
    private static final String STOPS_LIST = "stops_list";

    private static boolean easyMode = false;

    private boolean autoSelectDone = true;

    public ArrayList<String> cityList, routeNoList, stopList;
    public Spinner citySpinner, routeSpinner, stopSpinner, srcStopSpinner;
    public String Route = "", City = "";
    public Stop[] stops;
    public int checker = 1;
    public int deboardPos = -1;
    public int srcPos = -1;
    private ProgressDialog mProgressDialog;

    private EditText ip;
    private TextView homeText;
    private Button navigate, prevChoice;
    private LatLng currLoc;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        SharedPreferences appMode = getContext().getSharedPreferences(getString(R.string.appModePref), Context.MODE_PRIVATE);
        if (appMode.getInt(getString(R.string.appModePref), 0) > 0 ) easyMode = true;
        else easyMode = false;

        View rootView;
        if ( easyMode ) {
            rootView = inflater.inflate(R.layout.home_easy, container, false);
        }
        else {
            rootView = inflater.inflate(R.layout.home, container, false);
        }

        if (savedInstanceState != null) {
            cityList = savedInstanceState.getStringArrayList(CITY_LIST);
            routeNoList = savedInstanceState.getStringArrayList(ROUTE_LIST);
            stops = (Stop[])savedInstanceState.getSerializable(STOPS_LIST);
            stopList = (ArrayList)CommonUtils.getStringArray(stops);
        } else {
            cityList = new ArrayList<String>();
            routeNoList = new ArrayList<String>();
            stopList = new ArrayList<String>();
            cityList.add(getString(R.string.selectCity));
            routeNoList.add(getString(R.string.cityFirst));
            stopList.add(getString(R.string.routeFirst));
        }

        citySpinner = (Spinner) rootView.findViewById(R.id.citySpinner);
        routeSpinner = (Spinner) rootView.findViewById(R.id.routeNoSpinner);
        stopSpinner = (Spinner) rootView.findViewById(R.id.stopSpinner);
        srcStopSpinner = (Spinner) rootView.findViewById(R.id.srcStopSpinner);


        ArrayAdapter<String> cityAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);
        citySpinner.setOnItemSelectedListener(cityListener);

        if (cityList.size() == 2) {
            citySpinner.setSelection(1);
        }

        ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeNoList);
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeSpinner.setAdapter(routeAdapter);
        routeSpinner.setOnItemSelectedListener(routeListener);

        ArrayAdapter<String> stopAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stopSpinner.setAdapter(stopAdapter);
        stopSpinner.setOnItemSelectedListener(stopListener);
        srcStopSpinner.setOnItemSelectedListener(srcStopListener);


        navigate = (Button) rootView.findViewById(R.id.searchNavigate);
        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (City == "") {
                    CommonUtils.toast(getString(R.string.selectCity));
                } else if (Route == "") {
                    CommonUtils.toast(getString(R.string.selectRoute));
                } else {
                    if (isFavorite(Route)) {
                        loadStops(true);
                    } else {
                        if (srcPos == -1 || deboardPos == -1) {
                            CommonUtils.toast("Please select source and destination");
                        }
                        else favAlert();
                    }
                }
            }
        });

        if ( easyMode ) {
            homeText = (TextView) rootView.findViewById(R.id.homeText);
            homeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( citySpinner.getVisibility() == View.VISIBLE ) citySpinner.performClick();
                    else if ( routeSpinner.getVisibility() == View.VISIBLE ) routeSpinner.performClick();
                    else if ( srcStopSpinner.getVisibility() == View.VISIBLE ) srcStopSpinner.performClick();
                    else if ( stopSpinner.getVisibility() == View.VISIBLE ) stopSpinner.performClick();
                }
            });

            prevChoice = (Button) rootView.findViewById(R.id.searchPrevChoice);
            prevChoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( routeSpinner.getVisibility() == View.VISIBLE ) {
                        homeText.setText(R.string.easySelectCity);
                        routeSpinner.setVisibility(View.GONE);
                        citySpinner.setVisibility(View.VISIBLE);
                        prevChoice.setEnabled(false);
                    }
                    else if ( srcStopSpinner.getVisibility() == View.VISIBLE ) {
                        homeText.setText(R.string.easySelectRoute);
                        srcStopSpinner.setVisibility(View.GONE);
                        routeSpinner.setVisibility(View.VISIBLE);
                    }
                    else if ( stopSpinner.getVisibility() == View.VISIBLE ) {
                        homeText.setText(R.string.easySelectSrc);
                        stopSpinner.setVisibility(View.GONE);
                        srcStopSpinner.setVisibility(View.VISIBLE);
                        navigate.setEnabled(false);
                    }
                }
            });
        }

        Button refresh = (Button) rootView.findViewById(R.id.searchRefresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
            }
        });

        CommonUtils.initLocation(getActivity());
        return rootView;
    }

    private void showProgressDialog(String title, String message) {
        if (getActivity() == null)
            return;
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(getActivity(), title, message);
            mProgressDialog.setIndeterminate(true);
        } else {
            mProgressDialog.setTitle(title);
            mProgressDialog.setMessage(message);
            mProgressDialog.show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(CITY_LIST, cityList);
        outState.putStringArrayList(ROUTE_LIST, routeNoList);
        outState.putSerializable(STOPS_LIST, stops);
    }

    private OnItemSelectedListener cityListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            routeNoList.clear();
            routeNoList.trimToSize();

            if (pos == 0) {
                routeNoList.add(getString(R.string.cityFirst));
                City = "";
                routeSpinner.setEnabled(false);
            } else {
                routeNoList.add(getString(R.string.selectRoute));
                City = CommonUtils.deCapitalize(cityList.get(pos));

                String URL = Constants.SERVER_ROOT + "get_nearest_routes/" + City +
                        "?lat=" + String.valueOf(currLoc.latitude) +
                        "&lon=" + String.valueOf(currLoc.longitude) +
                        "&dist=" + String.valueOf(Constants.ERROR_RADIUS);

                StringRequest jsonArrayRequest = new StringRequest(Request.Method.GET, URL ,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (getActivity() == null)
                                    return;
                                try {
                                    JSONArray jsonArray = new JSONArray(response);
                                    for (int i = 0; i<jsonArray.length(); i++) {
                                        String temp = jsonArray.getString(i);
                                        routeNoList.add(temp);
                                    }
                                }catch (Exception ex) {
                                    CommonUtils.toast("Unable to get nearest routes");
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if ( getActivity() == null ) return;
                        CommonUtils.toast("Unable to get nearest routes");
                    }
                });
                trApp.getRequestQueue().add(jsonArrayRequest);

                JsonArrayRequest objRequest = new JsonArrayRequest(Constants.SERVER_ROOT + "get_routes/" + City,
                        new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jArray) {
                        CommonUtils.log(jArray.toString());
                        if (getActivity() == null)
                            return;
                        mProgressDialog.dismiss();
                        try {
                            for (int i = 0; i<jArray.length(); i++) {
                                JSONObject obj = jArray.getJSONObject(i);
                                String temp = obj.getString("route");
                                routeNoList.add(temp);
                                routeSpinner.setEnabled(true);
                            }
                            if (easyMode) {
                                if (PrefManager.isTTSEnabled()) {
                                    trApp.getTTS().speak(getString(R.string.easyGuideRoute), TextToSpeech.QUEUE_FLUSH, null);
                                }
                                homeText.setText(R.string.easySelectRoute);
                                citySpinner.setVisibility(View.GONE);
                                routeSpinner.setVisibility(View.VISIBLE);
                                prevChoice.setEnabled(true);
                            }
                        }catch (Exception ex) {
                            CommonUtils.toast("Unable to parse server response");
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (getActivity() == null)
                            return;
                        mProgressDialog.dismiss();
                        CommonUtils.toast(error.toString());
                    }
                });
                trApp.getRequestQueue().add(objRequest);
                showProgressDialog("Loading...", getContext().getString(R.string.loadingRoute));
            }

            ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeNoList);
            routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeSpinner.setAdapter(routeAdapter);

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            routeNoList.clear();
            routeNoList.trimToSize();

            routeNoList.add(getString(R.string.cityFirst));

            City = "";

            routeSpinner.setEnabled(false);

            ArrayAdapter<String> routeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, routeNoList);
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
                stopList.add(getString(R.string.selectStop));
                if (easyMode) {
                    if ( PrefManager.isTTSEnabled() ) {
                        trApp.getTTS().speak(getString(R.string.easyGuideSrc), TextToSpeech.QUEUE_FLUSH, null);
                    }
                    homeText.setText(R.string.easySelectSrc);
                    routeSpinner.setVisibility(View.GONE);
                    srcStopSpinner.setVisibility(View.VISIBLE);
                }

                stopSpinner.setEnabled(true);
                srcStopSpinner.setEnabled(true);
                Route = routeNoList.get(pos);
                loadStops(false);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
                ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, stopList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stopSpinner.setAdapter(adapter);
                srcStopSpinner.setAdapter(srcAdapter);
            } else {
                stopList.add(getString(R.string.routeFirst));
                stopSpinner.setEnabled(false);
                srcStopSpinner.setEnabled(false);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                ArrayAdapter<String> srcAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
                srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                stopSpinner.setAdapter(adapter);
                srcStopSpinner.setAdapter(srcAdapter);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Route = "";

            stopList.clear();
            stopList.trimToSize();

            stopList.add(getString(R.string.routeFirst));

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, stopList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stopSpinner.setAdapter(adapter);
        }
    };

    private OnItemSelectedListener srcStopListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            srcPos = position-1;
            if (position > 0 && easyMode) {
                if (PrefManager.isTTSEnabled()) {
                    trApp.getTTS().speak(getString(R.string.easyGuideDest),TextToSpeech.QUEUE_FLUSH,null);
                }
                homeText.setText(R.string.easySelectDest);
                srcStopSpinner.setVisibility(View.GONE);
                stopSpinner.setVisibility(View.VISIBLE);
                navigate.setEnabled(true);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            srcPos = -1;
        }
    } ;

    private OnItemSelectedListener stopListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            deboardPos = pos - 1;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            deboardPos = -1;
        }
    };


    public void addFav() {
        if (City == "") {
            CommonUtils.toast(getString(R.string.selectCity));
        } else if (Route == ""){
            CommonUtils.toast(getString(R.string.selectRoute));
        }

        DbHelper db = new DbHelper(getContext(),City, "route_"+CommonUtils.deCapitalize(Route));
        db.setTable(stops);
        db.closeDB();
    }

    private boolean isFavorite(String route) {
        DbHelper db = new DbHelper(getContext(),City);
        Cursor c = db.getTables();

        Boolean flag = false;
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String temp = c.getString(0);
                if (temp.toLowerCase().equals("route_"+route.toLowerCase())) {
                    flag = true;
                    break;
                }
                c.moveToNext();
            }
        }
        db.closeDB();

        return flag;
    }


    public void favAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("Add to Favorites?");

        // Setting Dialog Message
        alertDialog.setMessage("Would you like to add this route to your Favorite?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                addFav();
                loadStops(true);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                loadStops(true);
            }
        });

        alertDialog.show();
    }

    public void init() {
        cityList.clear();
        cityList.trimToSize();
        cityList.add(getString(R.string.selectCity));
        JsonArrayRequest objReq = new JsonArrayRequest(Constants.SERVER_ROOT + "get_cities",
                new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jArray) {
                CommonUtils.log(jArray.toString());
                if (getActivity() == null)
                    return;
                mProgressDialog.dismiss();
                try {
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject obj = jArray.getJSONObject(i);
                        cityList.add(CommonUtils.capitalize(obj.getString("name")));
                    }
                }catch (Exception ex) {
                    CommonUtils.toast("Error parsing response from server! Please try again");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                CommonUtils.log(error.toString());
                CommonUtils.toast(error.toString());
                if (getActivity() == null)
                    return;
                mProgressDialog.dismiss();
            }
        });
        trApp.getRequestQueue().add(objReq);
        showProgressDialog("Loading...", getContext().getString(R.string.loadingCity));

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, cityList);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);

        autoSelectDone = false;
        CommonUtils.initLocation(getActivity());

        if (easyMode) {
            if (PrefManager.isTTSEnabled()) {
                trApp.getTTS().speak(getString(R.string.easyGuideCity),TextToSpeech.QUEUE_FLUSH, null);
            }
            homeText.setText(R.string.easySelectCity);
            routeSpinner.setVisibility(View.GONE);
            srcStopSpinner.setVisibility(View.GONE);
            stopSpinner.setVisibility(View.GONE);
            citySpinner.setVisibility(View.VISIBLE);
            prevChoice.setEnabled(false);
            navigate.setEnabled(false);
        }
    }

    private void loadStops(final boolean isNavigate ) {
        if ( !isNavigate ) {
            JsonArrayRequest objRequest = new JsonArrayRequest(Constants.SERVER_ROOT + "get_stops/" + CommonUtils.deCapitalize(City) + "?route=" + Route,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jArray) {
                            if (getActivity() == null)
                                return;
                            mProgressDialog.dismiss();
                            stops = new Gson().fromJson(jArray.toString(), new Stop[]{}.getClass());
                            if (stops == null) {
                                CommonUtils.toast("Unable to parse server response");
                                return;
                            }
                            for (int i = 0; i < stops.length; i++) {
                                stopList.add(stops[i].getStop_name());
                            }

                            CommonUtils.initLocation(getActivity());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (getActivity() == null)
                        return;
                    mProgressDialog.dismiss();
                    CommonUtils.toast(error.toString());
                }
            });
            trApp.getRequestQueue().add(objRequest);
            showProgressDialog("Loading...", getContext().getString(R.string.loadingStop));
        } else {
            if (srcPos == -1 || deboardPos == -1) {
                CommonUtils.toast("Please select source and destination");
                return;
            }
            if (srcPos == deboardPos) {
                CommonUtils.toast(getString(R.string.sameSrcDest));
            }
            Intent navitaionActivity = new Intent(getActivity(), NavigateActivity.class);
            navitaionActivity.putExtra(NavigateActivity.STOPS, stops);
            navitaionActivity.putExtra(NavigateActivity.SRC_STOP, srcPos);
            navitaionActivity.putExtra(NavigateActivity.DST_STOP, deboardPos);
            startActivity(navitaionActivity);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event) {
        currLoc = event.getLocation();
        if ( !autoSelectDone ) {
            autoSelect(event.getLocation());
        }
        if (stops == null || stops.length <= 0) {
            return;
        }
        if (srcStopSpinner.getSelectedItemPosition() > 0)
            return;
        int pos = CommonUtils.getStopPos(stops, event.getLocation());
        srcStopSpinner.setSelection(pos+1);
        if (pos == -1 && checker == 1) {
                checker = 2;
            CommonUtils.toast("You are not near any bus stop on this route");
            if (PrefManager.isTTSEnabled()) {
                trApp.getTTS().speak("You are not near any bus stop on this route.", TextToSpeech.QUEUE_FLUSH, null);
            }
            return;
        }
    }

    private void autoSelect(LatLng loc) {
        Geocoder geo = new Geocoder(getContext(), Locale.US);
        try {
            List<Address> address = geo.getFromLocation(loc.latitude, loc.longitude, 1);
            String decodeCity = address.get(0).getLocality();

            for (int i = 0; i < cityList.size(); i++ ) {
                if ( cityList.get(i).toLowerCase().equals(decodeCity.toLowerCase()) ) {
                    citySpinner.setSelection(i);
                    autoSelectDone = true;
                    break;
                }
            }

            if ( !autoSelectDone ) {
                CommonUtils.toast(getContext().getString(R.string.autoselect_fail));
                autoSelectDone = true;
            }
        }
        catch ( Exception e) {
            CommonUtils.toast(getContext().getString(R.string.loc__decode_fail));
        }
    }
}
