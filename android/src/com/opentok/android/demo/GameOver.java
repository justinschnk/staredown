package com.opentok.android.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;

/**
 * Created with IntelliJ IDEA.
 * User: vivekpanyam
 * Date: 7/28/13
 * Time: 9:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class GameOver extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameover);

        Button b2 = (Button) findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(GameOver.this, LeaderboardActivity.class);
                startActivity(intent);
            }
        });
    }
}
