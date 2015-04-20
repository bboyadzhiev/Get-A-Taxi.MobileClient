package com.getataxi.client.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.getataxi.client.R;
import com.getataxi.client.comm.models.LocationDM;

import java.util.List;

/**
 * Created by bvb on 19.4.2015 г..
 */
public class LocationListViewAdapter  extends ArrayAdapter<LocationDM> {
    List<LocationDM> locationDMs;
    Context context;
    View detailView;
    int currentPosition;

    public LocationListViewAdapter(Context context, int resource,
                                  List<LocationDM> objects) {
        super(context, resource, objects);
        this.context = context;
        this.locationDMs = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem holder;

        if (convertView == null) {
            holder = new ViewHolderItem();

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.location_list_item, parent,
                    false);

            holder.locationTitle = (TextView) convertView
                    .findViewById(R.id.locationTitle);
            holder.locationAddress = (TextView) convertView
                    .findViewById(R.id.locationAddress);
            holder.locationLat = (TextView) convertView
                    .findViewById(R.id.locationLat);
            holder.locationLon = (TextView) convertView
                    .findViewById(R.id.locationLon);
            convertView.setTag(holder);
            this.currentPosition = position;

        } else {
            holder = (ViewHolderItem) convertView.getTag();
        }

        holder.locationTitle.setText(this.locationDMs.get(position).title);
        holder.locationAddress.setText(this.locationDMs.get(position).address);
        holder.locationLat.setText(Double.toString( this.locationDMs.get(position).latitude));
        holder.locationLon.setText(Double.toString( this.locationDMs.get(position).longitude));
        return convertView;
    }

    static class ViewHolderItem {
        TextView locationTitle;
        TextView locationAddress;
        TextView locationLat;
        TextView locationLon;
    }
}
