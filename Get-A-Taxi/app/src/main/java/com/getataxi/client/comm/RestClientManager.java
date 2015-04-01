package com.getataxi.client.comm;

import android.content.Context;
import android.content.SharedPreferences;

import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.getataxi.client.utils.UserPreferencesManager;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by bvb on 1.4.2015 г..
 */
public class RestClientManager {
    private static final String User_Info_File = "GetATaxiClient";
    private static final String BASE_URL = "http://get-a-taxi.apphb.com";
    private RestClient client;
    private Context context;

    public RestClientManager(Context ctx){
        this.context = ctx;
        this.client = new RestClient(BASE_URL);
    }



    private List<NameValuePair> getAuthorisationHeaders(){
        String token = UserPreferencesManager.getToken(this.context);
        List<NameValuePair> headers = new ArrayList<NameValuePair>();
        headers.add(new BasicNameValuePair("Authorization", "Bearer " + token));
        return headers;
    }

    public void login(String userName, String password){
        this.client.getAccountService(getAuthorisationHeaders()).login(userName, password, new Callback<LoginUserDM>(){

            @Override
            public void success(LoginUserDM loginUserDM, Response response) {
                int status  = response.getStatus();
                if (status == HttpStatus.SC_OK){
                    try {
                        UserPreferencesManager.saveLoginData(loginUserDM, context);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    public void register(final RegisterUserDM userDM){
        this.client.getAccountService(getAuthorisationHeaders()).register(userDM, new Callback<LoginUserDM>() {
            @Override
            public void success(LoginUserDM loginUserDM, Response response) {
                int status  = response.getStatus();
                if (status == HttpStatus.SC_OK){
                    try {
                        UserPreferencesManager.saveUserData(userDM, context);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    public static boolean checkForLogin(Context context){
        SharedPreferences userPref = context.getSharedPreferences(User_Info_File, 0);
        boolean isLogged = userPref.getBoolean("isLogged", false);
        return isLogged;
    }
}
