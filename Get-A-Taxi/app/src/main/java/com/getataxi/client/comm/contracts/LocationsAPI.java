package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.LocationDM;

import java.util.List;


import retrofit.Callback;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by bvb on 29.3.2015 г..
 */
public interface LocationsAPI {
    @GET("/api/Locations")
    void  getLocations(Callback<List<LocationDM>> callback);

    @GET("/api/Locations/{id}")
    void getLocation(@Path("id") int id, Callback<LocationDM> callback);

    @POST("/api/Locations")
    void addLocation(LocationDM locationDM, Callback<LocationDM> callback);

    @PUT("/api/Locations")
    void updateLocation(LocationDM locationDM, Callback<LocationDM> callback);

    @DELETE("/api/Locations/{id}")
    void deleteLocation(@Path("id") int id, Callback<LocationDM> callback);
}
