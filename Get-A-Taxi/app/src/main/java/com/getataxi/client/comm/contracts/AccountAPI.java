package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;

/**
 * Created by bvb on 29.3.2015 г..
 */

public interface AccountAPI {
    @POST("/api/Account/Login")
     void login(String userName, String password, Callback<LoginUserDM> callback);

    @POST("/api/Account/Register")
     void register(RegisterUserDM userInfo, Callback<LoginUserDM> callback);
}
