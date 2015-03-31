package com.getataxi.client.comm.models;

import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;
/**
 * Created by bvb on 29.3.2015 г..
 */
@Parcel
public class LoginUserDM {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("token_type")
    public String password;

    @SerializedName("expires_in")
    public String expiresAfter;

    @SerializedName("userName")
    public String username;

    @SerializedName(".issued")
    public String taken;

    @SerializedName(".expires")
    public String expires;
}
