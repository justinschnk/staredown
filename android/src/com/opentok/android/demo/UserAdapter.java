package com.opentok.android.demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class UserAdapter extends ArrayAdapter<NetApi.User> {

    Context context;
    int layoutResourceId;
    List<NetApi.User> data;
    Typeface tf;

    public UserAdapter(Context context, int layoutResourceId, List<NetApi.User> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        tf = Typeface.createFromAsset(context.getAssets(),
                "font.ttf");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        NetApi.User user = data.get(position);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            row.setTag(user);
        }



        TextView txtTitle = (TextView)row.findViewById(R.id.txtTitle);
        TextView wins = (TextView)row.findViewById(R.id.winloss);
        TextView staretime = (TextView)row.findViewById(R.id.staretime);
        txtTitle.setTypeface(tf);
        wins.setTypeface(tf);
        staretime.setTypeface(tf);

        txtTitle.setText(user.name);
        wins.setText(user.wins + "/" + user.losses + " W/L");

        staretime.setText((user.stareTime / 1000) + " seconds");
        return row;
    }


}