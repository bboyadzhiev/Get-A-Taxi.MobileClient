package com.getataxi.client.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.getataxi.client.comm.models.OrderDM;
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
    public static DateFormat tokenDateFormat = new SimpleDateFormat(Constants.TOKEN_DATE_FORMAT, Locale.ENGLISH);

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
    public static boolean tokenHasExpired(String expires) {
        if(expires == null || expires.isEmpty()){
            return true;
        }
        Date tokenExpirationDate = null;
        Date now =  new Date();
        try {
            tokenExpirationDate = GetDate(expires);
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

    public static String getBaseUrl(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getString(Constants.BASE_URL_STORAGE, Constants.DEFAULT_URL);
    }

    public static void setBaseUrl(Context context, String base_url){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString(Constants.BASE_URL_STORAGE, base_url);
        editor.commit();
    }

    public static void saveUserData(RegisterUserDM userDM, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String registerData = gson.toJson(userDM);
        editor.putString(Constants.USER_DATA, registerData);
        editor.putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }

    public static void saveLoginData(LoginUserDM userDM, Context context)
            throws IllegalStateException, IOException {
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String loginData = gson.toJson(userDM);
        editor.putString(Constants.LOGIN_DATA, loginData);
        editor.putBoolean(Constants.IS_LOGGED, true);
        editor.commit();
    }

    public static boolean checkForLoginCredentials(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String userData = userPrefs.getString(Constants.LOGIN_DATA, "");
        if(userData.length() > 0){
            return true;
        }
        return false;
    }

    public static boolean isLoggedIn(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getBoolean(Constants.IS_LOGGED, false);
    }

    public static void clearLoginData(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.remove(Constants.LOGIN_DATA);
        editor.putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }

    public static boolean checkForRegistration(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);

       // boolean email = userPref.contains("email");
       // boolean password = userPref.contains("password");
       // boolean isRegistered  =  email && password;
        return userPref.contains(Constants.USER_DATA);
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
        String userData = userPref.getString(Constants.LOGIN_DATA, "");
       // Log.d("USER_DATA", userData);
        Gson gson = new Gson();
        LoginUserDM userInfo = gson.fromJson(userData, LoginUserDM.class);
        return userInfo;
    }

    public static void logoutUser(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean(Constants.IS_LOGGED, false);
        editor.commit();
    }

    public static void storeLocations(List<LocationDM> locationDMList, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<LocationDM>>(){}.getType();
        String locationsData = gson.toJson(locationDMList, listOfLocation);
        editor.putString(Constants.USER_LOCATIONS, locationsData);
        editor.commit();
    }

    public static List<LocationDM> loadLocations(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String locations = userPref.getString(Constants.USER_LOCATIONS, "");
        Gson gson = new Gson();
        Type listOfLocation = new TypeToken<List<LocationDM>>(){}.getType();
        List<LocationDM> locationsData = gson.fromJson(locations, listOfLocation);
        return locationsData;
    }

    @Deprecated
    public static void storeOrder(OrderDM order, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        Gson gson = new Gson();
        String orderData = gson.toJson(order);
        editor.putString(Constants.ORDER_DATA, orderData);
        editor.commit();
    }


    @Deprecated
    public static OrderDM loadOrder(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String orderString = userPref.getString(Constants.ORDER_DATA, "");
        if (orderString.isEmpty()){
            return null;
        }
        Gson gson = new Gson();
        OrderDM order = gson.fromJson(orderString, OrderDM.class);
        return order;
    }

    @Deprecated
    public static boolean isInActiveOrder(Context context){
        SharedPreferences userPref = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        String inActiveOrder = userPref.getString(Constants.IS_IN_ORDER, "");
        if (inActiveOrder.isEmpty()){
            return false;
        }
        Gson gson = new Gson();
        return gson.fromJson(inActiveOrder, Boolean.class);
    }

    // Order
    public static void storeOrderId(int orderId, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.LAST_ORDER_ID, orderId);
        editor.commit();
    }

    public static int getLastOrderId(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getInt(Constants.LAST_ORDER_ID, -1);
    }

    public static void clearOrder(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt(Constants.LAST_ORDER_ID, -1);
        editor.commit();
    }

    public static boolean hasActiveOrder(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getInt(Constants.LAST_ORDER_ID, -1) != -1;
    }

    //Tracking
    public static void setTrackingState(boolean trackingEnabled, Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putBoolean(Constants.TRACKING_ENABLED, trackingEnabled);
        editor.commit();
    }

    public static boolean getTrackingState(Context context){
        SharedPreferences userPrefs = context.getSharedPreferences(USER_LOGIN_INFO, 0);
        return userPrefs.getBoolean(Constants.TRACKING_ENABLED, false);
    }


}
