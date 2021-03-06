package com.getataxi.client.comm;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.comm.contracts.ClientOrdersAPI;
import com.getataxi.client.comm.contracts.LocationsAPI;
import com.getataxi.client.comm.contracts.PhotosAPI;
import com.getataxi.client.comm.contracts.TaxiAPI;
import com.getataxi.client.comm.contracts.TaxiStandsAPI;
import com.getataxi.client.comm.models.OrderDetailsDM;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.comm.models.OrderDM;
import com.getataxi.client.comm.models.PhotoDM;
import com.getataxi.client.comm.models.RegisterUserDM;
import com.getataxi.client.comm.models.TaxiDetailsDM;
import com.getataxi.client.comm.models.TaxiStandDM;
import com.getataxi.client.utils.Constants;
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

    public static String base_url = Constants.DEFAULT_URL;
    public static RestClient client = new RestClient(base_url);
    private static List<NameValuePair> headers = new ArrayList<NameValuePair>();

    public RestClientManager(){
    }

    // AUTHENTICATION
    private static List<NameValuePair> getAuthorisationHeaders(Context context){
        LoginUserDM loginData = UserPreferencesManager.getLoginData(context);


        if ( headers.isEmpty()){
            headers.add(new BasicNameValuePair("Authorization", "Bearer " + loginData.accessToken));
        }
        if (UserPreferencesManager.tokenHasExpired(loginData.expires)){
            updateToken(loginData, "password", context);
        }
        Log.d("RESTMANAGER: ", "HEADERS ADDED:");
        for (NameValuePair header : headers) {
            Log.d(header.getName()+" : ", header.getValue());
        }
        return headers;
    }

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
                String errorJson =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                Log.d("RESTMANAGER: ", "Token update FAILED!");
                Toast.makeText(context, errorJson, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void login(LoginUserDM loginUserDM, String grantType, Callback<LoginUserDM> callback){
        client.getAccountService(null).login(loginUserDM.userName, loginUserDM.password, grantType,callback);
    }

    public static void register(final RegisterUserDM registerUserDM, Callback<String> callback){
        client.getAccountService(null).register(registerUserDM, callback);
    }

    public static void logout(Context context, Callback<String> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        client.getAccountService(heads).logout(callback);
    }

    // LOCATIONS
    public static void getLocations(Context context, Callback<List<LocationDM>> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        client.getLocationsService(heads).getLocations(callback);
    }

    public static void getLocation(int locationId, Context context, Callback<LocationDM> callback){
        client.getLocationsService(getAuthorisationHeaders(context)).deleteLocation(locationId, callback);
    }

    public static void updateClientLocation(final LocationDM locationDM, Context context, Callback<LocationDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        LocationsAPI locationsApi = client.getLocationsService(heads);
        locationsApi.updateLocation(locationDM, callback);
    }

    public static void addLocation(LocationDM locationDM, Context context, Callback<LocationDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        LocationsAPI locationsApi = client.getLocationsService(heads);
        locationsApi.addLocation(locationDM, callback);
    }

    public static void deleteLocation(int locationId, Context context, Callback<LocationDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        LocationsAPI locationsApi = client.getLocationsService(heads);
        locationsApi.deleteLocation(locationId, callback);
    }

    // Orders
    public static void addOrder(OrderDM order, Context context, Callback<OrderDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        ClientOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.addOrder(order, callback);
    }

    public static void getClientOrders(int page, Context context, Callback<List<OrderDM>> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        ClientOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.getOrdersPage(page, callback);
    }

    public static void getOrder(int id, Context context, Callback<OrderDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        ClientOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.getOrder(id, callback);
    }

    public static void cancelOrder(int id, Context context, Callback<OrderDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        ClientOrdersAPI ordersAPI = client.getOrdersService(heads);
        ordersAPI.cancelOrder(id, callback);
    }

    // Taxi
    public static void getTaxiDetails(int id, Context context, Callback<TaxiDetailsDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiAPI taxiApi = client.getTaxiService(heads);
        taxiApi.getTaxiDetails(id, callback);
    }

    // TAXI STANDS
    public static void getTaxiStands(double lat, double lon, Context context, Callback<List<TaxiStandDM>> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        TaxiStandsAPI taxiStandsAPI = client.getTaxiStandsService(heads);
        taxiStandsAPI.getTaxiStandsByLocation(lat, lon, callback);
    }

    //Photos
    public static void getUserPhoto(Context context, Callback<PhotoDM> callback){
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        PhotosAPI photosAPI = client.getPhotosService(heads);
        photosAPI.getUserPhoto(callback);
    }

    public static void getPhoto(int id, Context context, Callback<PhotoDM> callback) {
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        PhotosAPI photosAPI = client.getPhotosService(heads);
        photosAPI.getPhoto(id, callback);
    }

    public static void addPhoto(PhotoDM photo, Context context, Callback<Integer> callback) {
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        PhotosAPI photosAPI = client.getPhotosService(heads);
        photosAPI.addPhoto(photo, callback);
    }

    public static void updatePhoto(PhotoDM photo, Context context, Callback<Integer> callback) {
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        PhotosAPI photosAPI = client.getPhotosService(heads);
        photosAPI.updatePhoto(photo, callback);
    }
    public static void removePhoto(int id, Context context, Callback<Integer> callback) {
        List<NameValuePair> heads = new ArrayList<>();
        heads.addAll(getAuthorisationHeaders(context));
        PhotosAPI photosAPI = client.getPhotosService(heads);
        photosAPI.deletePhoto(id, callback);
    }
}
