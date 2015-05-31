package com.getataxi.client.comm;

import com.getataxi.client.comm.contracts.AccountAPI;
import com.getataxi.client.comm.contracts.ClientOrdersAPI;
import com.getataxi.client.comm.contracts.LocationsAPI;

import com.getataxi.client.comm.contracts.TaxiAPI;
import com.getataxi.client.comm.contracts.TaxiStandsAPI;
import com.getataxi.client.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by bvb on 31.3.2015 г..
 */
public class RestClient{

    private String baseUrl;
    private AccountAPI accountService;
    private ClientOrdersAPI ordersService;
    private LocationsAPI locationsService;
    private TaxiAPI taxiService;
    private TaxiStandsAPI taxiStandsService;
    private List<NameValuePair> headers;


    public RestClient(String baseUrl)
    {
        this.headers = new ArrayList<NameValuePair>();
        this.baseUrl = baseUrl;
        Gson gson = new GsonBuilder()
                // .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS")
                .create();

        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Content-Type", "application/json");
                if(!headers.isEmpty()){
                    for (NameValuePair header : headers) {
                        request.addHeader(header.getName(), header.getValue());
                    }
                }
            }
        };

//        final OkHttpClient okHttpClient = new OkHttpClient();
//        okHttpClient.setReadTimeout(Constants.READ_TIMEOUT, TimeUnit.MILLISECONDS);
//        okHttpClient.setWriteTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS);
//        okHttpClient.setConnectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        final RetrofitHttpClient client = new RetrofitHttpClient();


        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(this.baseUrl)
                .setConverter(new GsonConverter(gson))
                //.setClient(new OkClient(okHttpClient))
                .setClient(client)
                .setRequestInterceptor(requestInterceptor)
                .build();

        accountService = restAdapter.create(AccountAPI.class);
        ordersService = restAdapter.create(ClientOrdersAPI.class);
        locationsService = restAdapter.create(LocationsAPI.class);
        taxiService = restAdapter.create(TaxiAPI.class);
        taxiStandsService = restAdapter.create(TaxiStandsAPI.class);
    }

    public AccountAPI getAccountService(List<NameValuePair> headers){
        this.headers.clear();

        if (headers != null) {
            this.headers = headers;
        }
        return accountService;
    }

    public ClientOrdersAPI getOrdersService(List<NameValuePair> headers){
        this.headers.clear();
        if (headers != null) {
            this.headers = headers;
        }
        return ordersService;
    }

    public LocationsAPI getLocationsService(List<NameValuePair> headersPassed){
      //  this.headers.clear();
        if (headersPassed != null && !headersPassed.isEmpty()) {
            this.headers = headersPassed;
        }
        return locationsService;
    }

    public TaxiAPI getTaxiService(List<NameValuePair> heads){
        if (this.headers != null) {
            this.headers.clear();
            this.headers = heads;
        } else {
            this.headers = new ArrayList<NameValuePair>();
            this.headers.addAll(heads);
        }
        return taxiService;
    }

    public TaxiStandsAPI getTaxiStandsService(List<NameValuePair> heads){
        if (this.headers != null) {
            this.headers.clear();
            this.headers = heads;
        } else {
            this.headers = new ArrayList<NameValuePair>();
            this.headers.addAll(heads);
        }
        return taxiStandsService;
    }
}
