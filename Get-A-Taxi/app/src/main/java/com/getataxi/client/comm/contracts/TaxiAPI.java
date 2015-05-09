package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.TaxiDetailsDM;


import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by bvb on 9.5.2015 г..
 */
public interface TaxiAPI {
    @GET("/api/Taxi/{id}")
    void getTaxiDetails(@Path("id") int id, Callback<TaxiDetailsDM> callback);
}
