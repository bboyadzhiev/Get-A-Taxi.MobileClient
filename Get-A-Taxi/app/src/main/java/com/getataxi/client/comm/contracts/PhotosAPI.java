package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.PhotoDM;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by Bobi on 8/12/2015.
 */
public interface PhotosAPI {
    @GET("/api/Photos")
    void  getUserPhoto(Callback<PhotoDM> callback);

    @GET("/api/Photos/{id}")
    void getPhoto(@Path("id") int id, Callback<PhotoDM> callback);

    @POST("/api/Photos")
    void addPhoto(@Body PhotoDM photoDM, Callback<Integer> callback);

    @PUT("/api/Photos")
    void updatePhoto(@Body PhotoDM photoDM, Callback<Integer> callback);

    @DELETE("/api/Photos/{id}")
    void deletePhoto(@Path("id") int id, Callback<Integer> callback);
}
