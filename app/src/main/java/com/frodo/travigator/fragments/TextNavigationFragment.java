package com.frodo.travigator.fragments;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.frodo.travigator.R;
import com.frodo.travigator.activities.NavigateActivity;
import com.frodo.travigator.adapter.StopListAdapter;
import com.frodo.travigator.app.trApp;
import com.frodo.travigator.events.LocationChangedEvent;
import com.frodo.travigator.events.MocLocationChangedEvent;
import com.frodo.travigator.models.Stop;
import com.frodo.travigator.utils.CommonUtils;
import com.frodo.travigator.utils.LocationUtil;
import com.frodo.travigator.utils.PrefManager;
import com.google.android.gms.maps.model.LatLng;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TextNavigationFragment extends Fragment {

    private ListView stopsList ;
    private StopListAdapter stopListAdapter;
    private Stop[] stops;
    private int[] status;
    private int srcPos, dstPos, current = -1;
    private boolean isFirstTimeAdjusted = false;
    private int infoGivenPos = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.text_navigation_fragment, null);
        stopsList = (ListView)rootView.findViewById(R.id.stop_list);

        stopsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String message = "";
                if (current == -1) {
                    message = "We are still trying to detect your location.";
                }
                else {
                    if ( current == dstPos ) {
                        message = "You just missed your destination.";
                    }
                    else if ( current == dstPos - 1 ) {
                        message = "Next stop is your destination.";
                    }
                    else if ( current == dstPos - 2 ) {
                        message = "Next stop is " + stops[current+1].getStop_name() + ".\nThe stop after that is your destination.";
                    }
                    else {
                        message = "Next stop is " + stops[current+1].getStop_name() +", followed by ";
                        message += stops[current+2].getStop_name() + ".\n";
                        message += "You are " + String.valueOf(dstPos - current) + " stops away from your destination.";
                    }
                }

                HashMap<String,String> myHashAlarm = new HashMap();
                myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                        String.valueOf(AudioManager.STREAM_NOTIFICATION));
                CommonUtils.log(message);
                if (PrefManager.isTTSEnabled() ) {
                    trApp.getTTS().speak(message,TextToSpeech.QUEUE_FLUSH, myHashAlarm);
                }
                else {
                    CommonUtils.toast(message);
                }
            }
        });

        status = new int[stops.length];

        for (int i = 0 ; i < stops.length ; i++) {
            if (i < srcPos || i > dstPos)
                status[i] = StopListAdapter.STATUS_INACTIVE;
            else
                status[i] = StopListAdapter.STATUS_REMANING;
        }

        stopListAdapter = new StopListAdapter(getContext(), stops, status);
        stopsList.setAdapter(stopListAdapter);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        stops = (Stop[]) getActivity().getIntent().getSerializableExtra(NavigateActivity.STOPS);
        if (stops == null) {
            getActivity().finish();
        }
        srcPos = getActivity().getIntent().getIntExtra(NavigateActivity.SRC_STOP, -1);
        dstPos = getActivity().getIntent().getIntExtra(NavigateActivity.DST_STOP, -1);

        if ( srcPos > dstPos ) {
            List<Stop> list = Arrays.asList(stops);
            Collections.reverse(list);
            stops = list.toArray(stops);
            srcPos = stops.length - srcPos - 1;
            dstPos = stops.length - dstPos - 1;
        }

        CommonUtils.initLocation(getActivity());
    }

    @Subscribe
    public void onLocationChangedEvent(LocationChangedEvent event) {
        LatLng latLng = event.getLocation();
        int pos = CommonUtils.getStopPos(stops, latLng);
        CommonUtils.log("Pos:"+pos);
        if (pos == -1 && !isFirstTimeAdjusted){
            isFirstTimeAdjusted = true;
            stopsList.smoothScrollToPosition(CommonUtils.getNearstStop(stops, latLng));
        } else {
            if (pos == -1) {
                if(current != -1) {
                    status[current] = StopListAdapter.STATUS_VISITED;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopListAdapter.changeStatus(status);
                            }
                        });
                    }
                }
                return;
            }
            stopsList.smoothScrollToPosition(pos);
            if (infoGivenPos != pos) {
                String message = "You have arrived at " + stops[pos].getStop_name()+".\n";

                if ( pos == dstPos ) {
                    message += "This is your destination.";
                }
                else if (pos == dstPos - 1){
                    message += "Next stop is your destination.";
                }
                else if ( pos == dstPos - 2 ) {
                    message = "Next stop is " + stops[pos+1].getStop_name() + ".\nThe stop after that is your destination.";
                }
                else {
                    message = "Next stop is " + stops[pos+1].getStop_name() +", followed by ";
                    message += stops[pos+2].getStop_name() + ".\n";
                    message += "You are " + String.valueOf(dstPos - pos) + " stops away from your destination.";
                }

                CommonUtils.toast(message);
            }

            infoGivenPos = pos;
            isFirstTimeAdjusted = false;
            if (current == -1)
                current = pos;
            if (current != pos) {
                  for ( int i = 0; i <= current; i++) {
                        status[i] = StopListAdapter.STATUS_VISITED;
                  }
            }
            current = pos;
            status[current] = StopListAdapter.STATUS_CURRENT;
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopListAdapter.changeStatus(status);
                    }
                });
            }
        }
    }

    @Subscribe
    public void onMocLocationChangedEvent(MocLocationChangedEvent event) {
        CommonUtils.log("Event: "+event.getLocation().toString());
        EventBus.getDefault().post(new LocationChangedEvent(event.getLocation()));
    }
}
