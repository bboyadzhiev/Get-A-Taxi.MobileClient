package com.getataxi.client.comm.models;

/**
 * Created by bvb on 29.3.2015 г..
 */

import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;

@Parcel
public class AuthenticatedUserDM extends LoginUserDM {

    @SerializedName("longitude")
    public float longitude;

    @SerializedName("latitude")
    public float latitude;

    @SerializedName("phoneNumber")
    public String phoneNumber;

    @SerializedName("email")
    public String email;

    @SerializedName("userId")
    public String userId;

}
