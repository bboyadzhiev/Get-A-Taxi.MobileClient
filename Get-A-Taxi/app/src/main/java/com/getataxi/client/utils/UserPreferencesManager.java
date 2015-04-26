package com.getataxi.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by bvb on 31.3.2015 г..
 */
public class UserPreferencesManager {
    private static final String USER_LOGIN_INFO = "";


    public static DateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
    //".issued":"Thu, 09 Apr 2015 20:48:26 GMT"
    public static DateFormat tokenDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    public static Date GetDate(String dateString) throws ParseException {
        Date date = new Date();

        if (dateString != null) {
            date = tokenDateFormat.parse(dateString);
        }

        return date;
    }


    /**
     * Checks if token has expired.
     * @return true if token has expired
     */
    public static boolean tokenHasExpired(LoginUserDM loginData) {
        if(loginData == null){
            return true;
        }
        Date tokenExpirationDate = null;
        Date now =  new Date();
        try {
            tokenExpirationDate = GetDate(loginData.expires);
            //now = tokenDateFormat.parse(tokenDateFormat.format(GetDate(null)));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (tokenExpirationDate != null){
            int expired = tokenExpirationDate.compareTo(now);
            if (expired <= 0 ){
                // Token has expired!
                return true;
            }
        }
        return false;
    }

    public static void saveUserData(RegisterUserDM userDM, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String registerData = gson.toJson(userDM);
        editor.putString("UserData", registerData);
        editor.putBoolean("isLogged", false);
        editor.commit();
    }

    public static void saveLoginData(LoginUserDM userDM, Context context)
            throws IllegalStateException, IOException {
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String loginData = gson.toJson(userDM);
        editor.putString("LoginData", loginData);
        editor.putBoolean("isLogged", true);
        editor.commit();
    }

    public static boolean checkForLoginCredentials(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String userData = userPrefs.getString("LoginData", "");
        if(userData.length() > 0){
            return true;
        }
        return false;
    }

    public static boolean isLoggedIn(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getBoolean("isLogged", false);
    }



    public static boolean checkForRegistration(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);

       // boolean email = userPref.contains("email");
       // boolean password = userPref.contains("password");
       // boolean isRegistered  =  email && password;
        return userPref.contains("UserData");
    }


    public static String getToken(Context context){

        LoginUserDM userInfo = getLoginData(context);
        return userInfo.token;
    }

    public static String getEmail(Context context){
        LoginUserDM userInfo = getLoginData(context);
        return userInfo.email;
    }

    public static String getPassword(Context context){
        LoginUserDM userInfo = getLoginData(context);
        return userInfo.password;
    }

//    private static LoginUserDM getLoginUserData(Context context) {
//        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
//        Gson gson = new Gson();
//        return gson.fromJson("LoginData", LoginUserDM.class);
//    }


    public static LoginUserDM getLoginData(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String userData = userPref.getString("LoginData", "");
       // Log.d("USER_DATA", userData);
        Gson gson = new Gson();
        LoginUserDM userInfo = gson.fromJson(userData, LoginUserDM.class);
        return userInfo;
    }

    public static void logoutUser(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("isLogged", false);
        editor.commit();
    }

    public static void storeLocations(List<LocationDM> locationDMList, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<LocationDM>>(){}.getType();
        String locationsData = gson.toJson(locationDMList, listOfLocation);
        editor.putString("Locations", locationsData);
        editor.commit();
    }

    public static List<LocationDM> loadLocations(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String locations = userPref.getString("Locations", "");
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<LocationDM>>(){}.getType();
        List<LocationDM> locationsData = gson.fromJson(locations, listOfLocation);
        return locationsData;
    }
}
