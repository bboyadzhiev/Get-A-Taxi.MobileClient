package com.getataxi.client.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.google.gson.Gson;

import java.io.IOException;

/**
 * Created by bvb on 31.3.2015 г..
 */
public class UserPreferencesManager {
    private static final String USER_LOGIN_INFO = "";

    public static void saveUserData(RegisterUserDM userDM, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String registerData = gson.toJson(userDM);
        editor.putString("UserData", registerData);
        editor.putBoolean("isLogged", false);
        editor.commit();
    }

    public static void saveLoginData(LoginUserDM userInfo, Context context)
            throws IllegalStateException, IOException {
        // LoginUserDM userInfo = gson.fromJson(response, LoginUserDM.class);
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String loginData = gson.toJson(userInfo);
        editor.putString("LoginData", loginData);
        editor.putBoolean("isLogged", true);
        editor.commit();
    }

    public static boolean checkForLogin(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        boolean isLogged = userPref.getBoolean("isLogged", false);
        return isLogged;
    }

    public static boolean checkForRegistration(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        boolean user = userPref.contains("userName");
        boolean pass = userPref.contains("password");
        boolean isRegistered  =  user && pass;
        return isRegistered;
    }

    public static String getToken(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String token = userPref.getString("token", "");
        return token;
    }

    public static String getUsername(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String username = userPref.getString("userName", "");
        return username;
    }

    public static String getPassword(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String password = userPref.getString("password", "");
        return password;
    }

    public static void logoutUser(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("isLogged", false);
        editor.commit();
    }
}
