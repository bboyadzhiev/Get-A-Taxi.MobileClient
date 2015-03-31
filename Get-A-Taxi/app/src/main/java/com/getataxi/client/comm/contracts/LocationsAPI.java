package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.LocationDM;

import java.util.List;


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
    public List<LocationDM> getLocations();

    @GET("/api/Locations/{id}")
    public LocationDM getLocation(@Path("id") int id);

    @POST("/api/Locations")
    public LocationDM addLocation(LocationDM locationDM);

    @PUT("/api/Locations")
    public LocationDM updateLocation(LocationDM locationDM);

    @DELETE("/api/Locations/{id}")
    public LocationDM deleteLocation(@Path("id") int id);
}
