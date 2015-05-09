package com.getataxi.client.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by bvb on 31.3.2015 г..
 */

@Parcel
public class ClientOrderDM {
    @SerializedName("orderId")
    public int orderId;

    @SerializedName("startLat")
    public double orderLatitude;

    @SerializedName("startLng")
    public double orderLongitude;

    @SerializedName("start")
    public String orderAddress;

    @SerializedName("endLat")
    public double destinationLatitude;

    @SerializedName("endLng")
    public double destinationLongitude;

    @SerializedName("endLng")
    public String destinationAddress;

    @SerializedName("custComment")
    public String userComment;
}
