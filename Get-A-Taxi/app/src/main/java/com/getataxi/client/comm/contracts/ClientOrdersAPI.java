package com.getataxi.client.comm.contracts;

import com.getataxi.client.comm.models.OrderDM;

import java.util.List;

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
    public List<OrderDM> getOrders();

    @GET("/api/ClientOrders/{page}")
    public List<OrderDM> getOrdersPage(@Path("page") int page);

    @GET("/api/ClientOrders/{id}")
    public OrderDM getOrder(@Path("id") int id);

    @POST("/api/ClientOrders")
    public OrderDM addOrder(OrderDM locationDM);

    @PUT("/api/ClientOrders")
    public OrderDM updateOrder(OrderDM locationDM);

    @DELETE("/api/ClientOrders/{id}")
    public OrderDM deleteOrder(@Path("id") int id);
}
