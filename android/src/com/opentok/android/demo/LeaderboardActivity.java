package com.opentok.android.demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vivekpanyam
 * Date: 7/28/13
 * Time: 6:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class LeaderboardActivity extends Activity implements  NetApiCallback
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaderboard);

        NetApi netApi = new NetApi(this, "http://ec2-54-227-163-21.compute-1.amazonaws.com:3000");
        netApi.getLeaderboard();
    }

    @Override
    public void queueCallback(QueueData queueData) {
    }

    @Override
    public void gamesCallback(String someHash) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void leaderboardCallback(List<NetApi.User> users) {
        UserAdapter adapter = new UserAdapter(this,
                R.layout.list_item, users);


        ListView listView1 = (ListView)findViewById(R.id.listView);
        listView1.setAdapter(adapter);

    }

    @Override
    public void blink() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
