package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.comm.models.TaxiStandDM;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by bvb on 29.3.2015 г..
 */
public interface TaxiStandsAPI {
    @GET("/api/TaxiStands")
    void  getTaxiStandsByLocation(@Query("lat") double lat, @Query("lon") double lon, Callback<List<TaxiStandDM>> callback);

    @GET("/api/TaxiStands/{id}")
    void getTaxiStand(@Path("id") int id, Callback<TaxiStandDM> callback);

    @GET("/api/TaxiStands/{page}")
    void getTaxiStandsPage(@Path("page") int page, Callback<List<TaxiStandDM>> callback);

    @POST("/api/TaxiStands")
    void getByLocation(@Body LocationDM locationDM, Callback<List<TaxiStandDM>> callback);

}
