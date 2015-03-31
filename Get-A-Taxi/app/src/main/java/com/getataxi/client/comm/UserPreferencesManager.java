package com.getataxi.client.comm;

import android.content.Context;
import android.content.SharedPreferences;

import com.getataxi.client.comm.models.LoginUserDM;
import com.google.gson.Gson;

import java.io.IOException;

/**
 * Created by bvb on 31.3.2015 г..
 */
public class UserPreferencesManager {
    private static final String USER_LOGIN_INFO = "";

    public static void saveUserData(String userName, String password, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString("username", userName);
        editor.putString("password", password);
        editor.putBoolean("isLogged", false);
        editor.commit();
    }

    public static void saveLoginData(String response, String password, Context context)
            throws IllegalStateException, IOException {
        Gson gson = new Gson();
        LoginUserDM userInfo = gson.fromJson(response, LoginUserDM.class);
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPref.edit();
        editor.putString("username", userInfo.username);
        editor.putString("password", password);
        editor.putString("token", userInfo.accessToken);
        editor.putString("expiration", userInfo.expires);
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
        boolean user = userPref.contains("username");
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
        String username = userPref.getString("username", "");
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
