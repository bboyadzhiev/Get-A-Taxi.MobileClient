package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.OrderDM;

import java.util.List;

import retrofit.Callback;
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
    void getOrders(Callback<List<OrderDM>> callback);

    @GET("/api/ClientOrders/{page}")
    void getOrdersPage(@Path("page") int page, Callback<List<OrderDM>> callback);

    @GET("/api/ClientOrders/{id}")
    void getOrder(@Path("id") int id, Callback<OrderDM> callback);

    @POST("/api/ClientOrders")
    void addOrder(OrderDM locationDM,  Callback<OrderDM> callback);

    @PUT("/api/ClientOrders")
    void updateOrder(OrderDM locationDM, Callback<OrderDM> callback);

    @DELETE("/api/ClientOrders/{id}")
    void cancelOrder(@Path("id") int id, Callback<OrderDM> callback);
}
