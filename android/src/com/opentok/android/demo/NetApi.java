package com.opentok.android.demo;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NetApi {
    private String mUrl;
    private NetApiCallback mNetApiCallback;

    private static final String TAG = "NetApi";

    public NetApi(NetApiCallback netApiCallback, String url) {
        mUrl = url;
        mNetApiCallback = netApiCallback;
    }

    public void getQueue(String id, String name) {
        String getUrl = mUrl + "/queue?id=" + id + "&name=" + name;
        new RequestTask("queue").execute(getUrl);
    }


    class RequestTask extends AsyncTask<String, String, String> {

        private String mEndpoint;

        public RequestTask(String endpoint) {
            mEndpoint = endpoint;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
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
    }
}
