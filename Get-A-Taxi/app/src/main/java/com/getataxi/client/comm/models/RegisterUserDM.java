package com.getataxi.client.comm.models;

/**
 * Created by bvb on 29.3.2015 г..
 */
import com.google.gson.annotations.SerializedName;
import org.parceler.Parcel;

@Parcel
public class RegisterUserDM {

    @SerializedName("email")
    public String email;

    @SerializedName("userName")
    public String username;

    @SerializedName("firstName")
    public String firstName;

    @SerializedName("middleName")
    public String middleName;

    @SerializedName("lastName")
    public String lastName;

    @SerializedName("phoneNumber")
    public String phoneNumber;

    @SerializedName("password")
    public String password;

    @SerializedName("confirmPassword")
    public String confirmPassword;
}
