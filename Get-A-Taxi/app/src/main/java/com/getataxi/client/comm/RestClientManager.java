package com.getataxi.client.comm;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.ClientOrderDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.getataxi.client.models.Location;
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
import retrofit.mime.TypedByteArray;

/**
 * Created by bvb on 1.4.2015 г..
 */
public class RestClientManager {
    private static final String User_Info_File = "GetATaxiClient";
    private static final String BASE_URL = "http://get-a-taxi.apphb.com";
    private static RestClient client = new RestClient(BASE_URL);
    private static List<NameValuePair> headers = new ArrayList<NameValuePair>();

    public RestClientManager(){
    }

    private static List<NameValuePair> getAuthorisationHeaders(Context context){
        LoginUserDM loginData = UserPreferencesManager.getLoginData(context);
        if (headers.isEmpty()){
            headers.add(new BasicNameValuePair("Authorization", "Bearer " + loginData.accessToken));
        }
        if (UserPreferencesManager.tokenHasExpired(loginData)){
            updateToken(loginData, "password", context);
        }
        Log.d("RESTMANAGER: ", "HEADERS");
        for (NameValuePair header : headers) {
            Log.d(header.getName()+" : ", header.getValue());
        }
        return headers;
    }

//  private Context context;
//
//    public RestClientManager(Context ctx){
//        this.context = ctx;
//        this.client = new RestClient(BASE_URL);
//    }
//
//
//    Callback callback = new Callback() {
//        @Override
//        public void success(Object o, Response response) {
//
//        }
//
//        @Override
//        public void failure(RetrofitError retrofitError) {
//
//        }
//    };
//
//
//    public void login(LoginUserDM loginUserDM, String grantType){
//        Toast.makeText(context, "email:" + loginUserDM.email + " pass: " + loginUserDM.password, Toast.LENGTH_LONG).show();
//
//        this.client.getAccountService(null).login(loginUserDM.userName, loginUserDM.password, grantType,new Callback<LoginUserDM>(){
//            @Override
//            public void success(LoginUserDM loginUserDM, Response response) {
//                int status  = response.getStatus();
//                if (status == HttpStatus.SC_OK){
//                    try {
//                        Resources res = context.getResources();
//                        String welcome = String.format(res.getString(R.string.login_success_message),
//                                loginUserDM.userName);
//                        Toast.makeText(context, welcome, Toast.LENGTH_LONG).show();
//                        Thread.sleep(5000);
//                        UserPreferencesManager.saveLoginData(loginUserDM, context);
//
//                        Toast.makeText(context, "Login data stored!", Toast.LENGTH_LONG).show();
//                        Intent orderMap = new Intent(context, OrderMap.class);
//                        orderMap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        context.startActivity(orderMap);
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//             //   TODO: Fix error message
//                String errorJson =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
//                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
//
//            }
//        });
//    }
//
//    public void register(final RegisterUserDM userDM){
//      // this.client.getAccountService(getAuthorisationHeaders()).register(userDM, new Callback<LoginUserDM>() {
//        this.client.getAccountService(null).register(userDM, new Callback<LoginUserDM>() {
//            @Override
//            public void success(LoginUserDM loginUserDM, Response response) {
//                int status  = response.getStatus();
//                if (status == HttpStatus.SC_OK){
//                    try {
//                        Toast.makeText(context, "Successfully registered", Toast.LENGTH_LONG).show();
//                        UserPreferencesManager.saveUserData(userDM, context);
//                        Intent login = new Intent(context, LoginActivity.class);
//                        context.startActivity(login);
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                //TODO: Fix error message
//                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
//            }
//        });
//    }

    public static void updateToken(LoginUserDM loginUserDM, String grantType, final Context context) {
        client.getAccountService(null).login(loginUserDM.email, loginUserDM.password, grantType,new Callback<LoginUserDM>(){
            @Override
            public void success(LoginUserDM loginUserDM, Response response) {
                int status  = response.getStatus();
                if (status == HttpStatus.SC_OK){
                    try {
                        headers.clear();
                        headers.add(new BasicNameValuePair("Authorization", "Bearer " + loginUserDM.accessToken));
                        UserPreferencesManager.saveLoginData(loginUserDM, context);
                        Log.d("RESTMANAGER: ", "Token UPDATED!");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                // TODO: Fix error message
                String errorJson =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                Log.d("RESTMANAGER: ", "Token update FAILED!");
                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
if (error.getResponse().getStatus() == 401){

}
            }
        });
    }

    public static void login(LoginUserDM loginUserDM, String grantType, Callback<LoginUserDM> callback){
        client.getAccountService(null).login(loginUserDM.email, loginUserDM.password, grantType,callback);
    }

    public static void register(final RegisterUserDM userDM, Callback<LoginUserDM> callback){
        client.getAccountService(null).register(userDM, callback);
    }

//    public static void login(LoginUserDM loginUserDM, String grantType, final Context context){
//        Toast.makeText(context, "email:" + loginUserDM.userName + " pass: " + loginUserDM.password, Toast.LENGTH_LONG).show();
//
//        //   this.client.getAccountService(getAuthorisationHeaders()).login(loginUserDM.userName, loginUserDM.password, grantType,new Callback<LoginUserDM>(){
//
//       client.getAccountService(null).login(loginUserDM.email, loginUserDM.password, grantType,new Callback<LoginUserDM>(){
//            @Override
//            public void success(LoginUserDM loginUserDM, Response response) {
//                int status  = response.getStatus();
//                if (status == HttpStatus.SC_OK){
//                    try {
//                        Resources res = context.getResources();
//                        String welcome = String.format(res.getString(R.string.login_success_message),
//                                loginUserDM.userName);
//                        Toast.makeText(context, welcome, Toast.LENGTH_LONG).show();
//                        Thread.sleep(5000);
//                        UserPreferencesManager.saveLoginData(loginUserDM, context);
//
//                        Toast.makeText(context, "Login data stored!", Toast.LENGTH_LONG).show();
//                        Intent orderMap = new Intent(context, OrderMap.class);
//                        orderMap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        context.startActivity(orderMap);
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                // TODO: Fix error message
//                String errorJson =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
//                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
//
//            }
//        });
//    }

//    public static void register(final RegisterUserDM userDM, final Context context){
//        client.getAccountService(null).register(userDM, new Callback<LoginUserDM>() {
//            @Override
//            public void success(LoginUserDM loginUserDM, Response response) {
//                int status  = response.getStatus();
//                if (status == HttpStatus.SC_OK){
//                    try {
//                        Toast.makeText(context, "Successfully registered", Toast.LENGTH_LONG).show();
//                        UserPreferencesManager.saveUserData(userDM, context);
//                        Intent login = new Intent(context, LoginActivity.class);
//                        context.startActivity(login);
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                // TODO: Fix error message
//                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
//            }
//        });
//    }

    // Locations
    public static void getLocations(Context context, Callback<List<LocationDM>> callback){
        client.getLocationsService(getAuthorisationHeaders(context)).getLocations(callback);
    }

    public static void getLocation(int locationId, Context context, Callback<LocationDM> callback){
        client.getLocationsService(getAuthorisationHeaders(context)).deleteLocation(locationId, callback);
    }

    public static void addLocation(final LocationDM locationDM, Context context, Callback<LocationDM> callback){
        client.getLocationsService(getAuthorisationHeaders(context)).addLocation(locationDM, callback);
    }

    public static void deleteLocation(int locationId, Context context, Callback<LocationDM> callback){
        client.getLocationsService(getAuthorisationHeaders(context)).deleteLocation(locationId, callback);
    }

    // Orders
    public static void getClientOrders(int page, Context context, Callback<List<ClientOrderDM>> callback){
        client.getOrdersService(getAuthorisationHeaders(context)).getOrdersPage(page, callback);
    }

    public static void getOrder(int page, Context context, Callback<ClientOrderDM> callback){

    }

    public static void updateClientLocation(final LocationDM locaton, Context context, Callback callback){
        client.getLocationsService(getAuthorisationHeaders(context)).updateLocation(locaton, callback);

    }

}
