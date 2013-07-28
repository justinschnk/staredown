package com.opentok.android.demo;

import android.os.AsyncTask;
import android.util.Log;
import com.opentok.android.demo.server.Api;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class NetApi {
    private String mUrl;
    private NetApiCallback mNetApiCallback;

    private static final String TAG = "NetApi";

    public class User {
        public String id;
        public String name;
        public int wins;
        public int losses;
        public int stareTime;
    }

    public class Game {
        public String id;
        public String uid1;
        public String uid2;
        public int duration;
        public int winner;
    }

    public NetApi(NetApiCallback netApiCallback, String url) {
        mUrl = url;
        mNetApiCallback = netApiCallback;
    }

    public void getQueue(String id, String name) {
        String getUrl = mUrl + "/queue?id=" + id + "&name=" + name;
        new RequestTask("queue").execute(getUrl);
    }

    public void sendGame(String someHash) {
        String getUrl = mUrl + "/startgame?game="+someHash;
        new RequestTask("games").execute(getUrl);

    public void getLeaderboard() {
        String getUrl = mUrl + "/leaderboard?json=true";
        new RequestTask("leaderboard").execute(getUrl);
    }


    class RequestTask extends AsyncTask<String, String, String> {

        private String mEndpoint;

        private String currentUri;

        public RequestTask(String endpoint) {
            mEndpoint = endpoint;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                currentUri = uri[0];
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            if(mEndpoint.equals("queue")) {
                QueueData queueData = getQueueDataFromJson(result);
                mNetApiCallback.queueCallback(queueData);
            } else if (mEndpoint.equals("leaderboard")) {
                mNetApiCallback.leaderboardCallback(getLeaderboard(result));
            }
            else if(mEndpoint.equals("games")) {
                String splits[] = currentUri.split("/");
                mNetApiCallback.gamesCallback(splits[splits.length-1].substring(15));
            }
        }

        private QueueData getQueueDataFromJson(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                return new QueueData(
                        jsonObject.getString("status"),
                        jsonObject.getString("sessionId"),
                        jsonObject.getString("token"),
                        jsonObject.getString("firebase")
                );
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: " + e);
                return null;
            }
        }


        private List<User> getLeaderboard(String result) {
            try {
                LinkedList<User> l = new LinkedList<User>();
                JSONArray array = new JSONArray(result);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    User user = new User();
                    user.name = object.getString("name");
                    user.losses = object.getInt("loses");
                    user.stareTime = object.getInt("stareTime");
                    user.wins = object.getInt("wins");
                    l.add(user);
                }
                return l;
            } catch (JSONException e) {
                Log.d(TAG, "JSONException: " + e);
                return null;
            }
        }
    }
}
