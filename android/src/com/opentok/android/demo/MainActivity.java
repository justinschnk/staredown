package com.opentok.android.demo;

import android.content.SharedPreferences;
import android.widget.Button;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.opentok.android.demo.controlbar.ControlBarActivity;
import com.opentok.android.demo.helloworld.HelloWorldActivity;

import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import java.util.Map;

import java.util.List;

/**
 * Main demo app for getting started with the OpenTok Android SDK.
 * It contains:
 * - a basic hello-world activity
 * - a basic hello-world activity with control bar with stream name and action buttons to switch camera and audio mute
 */
public class MainActivity extends Activity {

    private static final String LOGTAG = "demo-opentok-sdk";
    private WakeLock wakeLock;

    private final String TAG = "MainActivity";


    public static final String[] usernames = {
            "Apple",
            "Apricot",
            "Avocado",
            "Banana",
            "Breadfruit",
            "Bilberry",
            "Blackberry",
            "Blackcurrant",
            "Blueberry",
            "Boysenberry",
            "Currant",
            "Cherry",
            "Cherimoya",
            "Chili",
            "Cloudberry",
            "Coconut",
            "Damson",
            "Date",
            "Dragonfruit",
            "Durian",
            "Elderberry",
            "Feijoa",
            "Fig",
            "Gooseberry",
            "Grape",
            "Grapefruit",
            "Guava",
            "Huckleberry",
            "Honeydew",
            "Jackfruit",
            "Jettamelon",
            "Jambul",
            "Jujube",
            "Kiwi fruit",
            "Kumquat",
            "Legume",
            "Lemon",
            "Lime",
            "Loquat",
            "Lychee",
            "Mango",
            "Melon",
            "Canary melon",
            "Cantaloupe",
            "Honeydew",
            "Watermelon",
            "Rock melon",
            "Nectarine",
            "Nut",
            "Orange",
            "Clementine",
            "Mandarine",
            "Tangerine",
            "Papaya",
            "Peach",
            "Pepper",
            "Pear",
            "Persimmon",
            "Physalis",
            "Plum",
            "Prune",
            "Pineapple",
            "Pomegranate",
            "Pomelo",
            "Purple Mangosteen",
            "Quince",
            "Raspberry",
            "Western raspberry",
            "Rambutan",
            "Redcurrant",
            "Salal berry",
            "Satsuma",
            "Star fruit",
            "Strawberry",
            "Tamarillo",
            "Tomato",
            "Watermelon",
            "Ugli fruit"
    };

    public static String generateUsername() {
        StringBuilder sb = new StringBuilder();
        int i = (int) Math.floor(Math.random() * usernames.length);
        sb.append(usernames[i]);
        int i2 = (int) Math.floor(Math.random() * 99);
        sb.append(i2);
        return sb.toString();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
       
        Button b = (Button) findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startHelloWorldApp();
            }
        });

        Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
                startActivity(intent);
            }
        });

        SharedPreferences settings = getSharedPreferences("blinkprefs", 0);
        String name = settings.getString("name", "");
        if (!name.isEmpty()) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("name", generateUsername());
            editor.commit();
        }
       
        // Disable screen dimming
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onStop() {

        super.onStop();

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

    }
    @Override
    public void onResume() {

        super.onResume();

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Starts the Hello-World demo app. See HelloWorldActivity.java
     */
    public void startHelloWorldApp() {

        Log.i(LOGTAG, "starting hello-world app");

        Intent intent = new Intent(MainActivity.this, HelloWorldActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Starts the Control Bar demo app. See ControlBarActivity.java
     */
    public void startControlBarApp() {

        Log.i(LOGTAG, "starting control bar app");

        Intent intent = new Intent(MainActivity.this, ControlBarActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}