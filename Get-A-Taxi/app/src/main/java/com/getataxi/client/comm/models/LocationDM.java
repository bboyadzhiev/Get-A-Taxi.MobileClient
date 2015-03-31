package com.getataxi.client.comm.models;

import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;
/**
 * Created by bvb on 30.3.2015 г..
 */


@Parcel
public class LocationDM {

    @SerializedName("lat")
    public double latitude;

    @SerializedName("lon")
    public double longitude;

    @SerializedName("address")
    public String address;

    @SerializedName("title")
    public String title;
}
