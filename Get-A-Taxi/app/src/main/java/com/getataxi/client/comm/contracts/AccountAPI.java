package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by bvb on 29.3.2015 г..
 */

public interface AccountAPI {
    @FormUrlEncoded
    @POST("/api/Account/Login")
     void login(@Field("username") String userName,@Field("password") String password, @Field("grant_type") String grantType, Callback<LoginUserDM> callback);

    @POST("/api/Account/Register")
     void register(@Body RegisterUserDM userInfo, Callback<String> callback);

    @POST("/api/Account/Logout")
    void logout(Callback<String> callback);

}
