package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.AssignedOrderDM;
import com.getataxi.client.comm.models.ClientOrderDM;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by bvb on 31.3.2015 г..
 */
public interface ClientOrdersAPI {
    @GET("/api/ClientOrders")
    void getOrders(Callback<List<ClientOrderDM>> callback);

    @GET("/api/ClientOrders/{page}")
    void getOrdersPage(@Path("page") int page, Callback<List<ClientOrderDM>> callback);

    @GET("/api/ClientOrders/{id}")
    void getOrder(@Path("id") int id, Callback<AssignedOrderDM> callback);

    @POST("/api/ClientOrders")
    void addOrder(@Body ClientOrderDM locationDM,  Callback<ClientOrderDM> callback);

    @PUT("/api/ClientOrders")
    void updateOrder(@Body ClientOrderDM locationDM, Callback<ClientOrderDM> callback);

    @DELETE("/api/ClientOrders/{id}")
    void cancelOrder(@Path("id") int id, Callback<ClientOrderDM> callback);
}
