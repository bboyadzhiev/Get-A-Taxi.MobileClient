package com.getataxi.client.comm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.R;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.UserPreferencesManager;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.BasicAuthenticationCredentials;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;



/**
 * Created by bvb on 30.4.2015 г..
 */
public class SignalRNotificationService extends Service {

    private HubConnection connection;

    private HubProxy proxy;
    @Override
    public void onCreate() {
        super.onCreate();

        // Register for Location Service broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.LOCATION_UPDATED);
        registerReceiver(locationReceiver, filter);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Toast.makeText(this, "SignalR Service Start", Toast.LENGTH_LONG).show();

        int orderId = intent.getIntExtra(Constants.ORDER_ID, -1);

        if(orderId == -1){
            return;
        }

        String server = Constants.BASE_URL + Constants.HUB_ENDPOINT;

        Logger l  = new Logger() {
            @Override
            public void log(String s, LogLevel logLevel) {

            }
        };

        // Getting user details
        LoginUserDM loginData = UserPreferencesManager.getLoginData(getApplicationContext());

        // Prepare request
       // Request request = new Request("POST");
        connection = new HubConnection(server);
        proxy = connection.createHubProxy(Constants.HUB_PROXY);
        connection.setCredentials(new TokenAuthenticationCredentials(loginData.accessToken));
        //connection.prepareRequest(request);


        SignalRFuture<Void> awaitConnection = connection.start();
        try {
            awaitConnection.get();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        //String phoneNumber = tMgr.getLine1Number();
        proxy.invoke(Constants.HUB_CONNECT, orderId);

        //Then call on() to handle the messages when they are received.
        proxy.on("ok", new SubscriptionHandler1<String>() {
            @Override
            public void run(String msg) {
                Log.d("result := ", msg);
            }
        }, String.class);

        proxy.on(Constants.HUB_PEER_LOCATION_CHANGED, new SubscriptionHandler2<Double, Double>() {
            @Override
            public void run(Double aDouble, Double aDouble2) {

            }
        }, Double.class, Double.class);

                //--------------------------------------------------------------------------------
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(locationReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Location location;

    /**
     * The receiver for the Location Service location update broadcasts
     */
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {
                Bundle data = intent.getExtras();

                location = data.getParcelable(Constants.LOCATION);

                double lat = location.getLatitude();
                double lon = location.getLongitude();

                if ( proxy != null){
                    proxy.invoke(Constants.HUB_MY_LOCATION_CHANGED, lat, lon);
                }

            }
        }
    };
}
