package com.getataxi.client.comm;

import com.getataxi.client.comm.contracts.AccountAPI;
import com.getataxi.client.comm.contracts.ClientOrdersAPI;
import com.getataxi.client.comm.contracts.LocationsAPI;
import com.getataxi.client.comm.models.LocationDM;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by bvb on 31.3.2015 г..
 */
public class RestClient{
    private static final String BASE_URL = "http://get-a-taxi.apphb.com";
    private AccountAPI accountService;
    private ClientOrdersAPI ordersService;
    private LocationsAPI locationsService;

    public RestClient()
    {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(BASE_URL)
                .setConverter(new GsonConverter(gson))
                .build();

        accountService = restAdapter.create(AccountAPI.class);
        ordersService = restAdapter.create(ClientOrdersAPI.class);
        locationsService = restAdapter.create(LocationsAPI.class);
    }

    public AccountAPI getAccountService(){
        return accountService;
    }

    public ClientOrdersAPI getOrdersService(){
        return ordersService;
    }

    public LocationsAPI getLocationsService(){
        return locationsService;
    }
}
