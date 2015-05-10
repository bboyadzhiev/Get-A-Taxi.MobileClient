package com.getataxi.client.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by bvb on 9.5.2015 г..
 */
@Parcel
public class AssignedOrderDM extends ClientOrderDM {
    @SerializedName("taxiId")
    public int taxiId;
    @SerializedName("taxiPlate")
    public String taxiPlate;
    @SerializedName("driverPhone")
    public String driverPhone;
    @SerializedName("driverName")
    public String driverName;
}
