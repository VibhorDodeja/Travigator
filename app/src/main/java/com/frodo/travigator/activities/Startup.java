package com.frodo.travigator.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.frodo.travigator.R;
import com.frodo.travigator.app.trApp;
import com.frodo.travigator.utils.CommonUtils;
import com.frodo.travigator.utils.LocationUtil;


public class Startup extends ActionBarActivity {


    private Integer images[] = {R.drawable.first, R.drawable.second, R.drawable.third, R.drawable.fourth};
    private int currImage = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);

        setInitialImage();

        update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setInitialImage() {
        setCurrentImage();
    }


    public void update(){

            final Handler h = new Handler();

        final Runnable r4 = new Runnable() {

            @Override
            public void run() {
                SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.appModePref), Context.MODE_PRIVATE);
                int appMode = sp.getInt(getString(R.string.appModePref), -1);

                if ( appMode == -1 ) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Startup.this);
                    builder.setTitle(R.string.appMode).setItems(R.array.appModeArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.appModePref), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt(getString(R.string.appModePref), which);
                            editor.commit();

                            Intent i = new Intent(Startup.this, com.frodo.travigator.activities.ActionBarActivity.class);
                            startActivity(i);
                        }
                    });
                    builder.create().show();
                }
                else {
                    Intent i = new Intent(Startup.this, com.frodo.travigator.activities.ActionBarActivity.class);
                    startActivity(i);
                }
            }
        };

        final Runnable r3 = new Runnable() {

            @Override
            public void run() {
                currImage+=1;
                setCurrentImage();
		h.postDelayed(r4,300);
            }
        };


        final Runnable r2 = new Runnable() {

            @Override
            public void run() {
                currImage+=1;
                setCurrentImage();
                h.postDelayed(r3,300);
            }
        };

        Runnable r1 = new Runnable() {

            @Override
            public void run() {
                // do first thing
                currImage+=1;
                setCurrentImage();
                h.postDelayed(r2, 300); // 10 second delay
            }
        };

        h.postDelayed(r1, 300); // 5 second delay
    }


    private void setCurrentImage() {

        final ImageView imageView = (ImageView) findViewById(R.id.imageDisplay);
        imageView.setImageResource(images[currImage]);
    imageView.setScaleType(ImageView.ScaleType.FIT_XY);

    }


}