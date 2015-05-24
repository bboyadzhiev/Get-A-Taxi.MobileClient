package com.getataxi.client.comm.models;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/**
 * Created by bvb on 9.5.2015 г..
 */
@Parcel
public class TaxiDetailsDM {

    @SerializedName("taxiId")
    public int taxiId;

    @SerializedName("lat")
    public double latitude;

    @SerializedName("lon")
    public double longitude;

    @SerializedName("onDuty")
    public boolean onDuty;

    @SerializedName("isAvailable")
    public boolean isAvailable;

    @SerializedName("plate")
    public String plate;

    @SerializedName("driverName")
    public String driverName;

    @SerializedName("phone")
    public String phone;

    @SerializedName("districtId")
    public int districtId;

    @SerializedName("taxiStandId")
    public int taxiStandId;

    @SerializedName("taxiStandAlias")
    public String taxiStandAlias;

    @SerializedName("address")
    public String address;
}
