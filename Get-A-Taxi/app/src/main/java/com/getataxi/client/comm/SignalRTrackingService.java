package com.getataxi.client.comm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.R;
import com.getataxi.client.comm.models.LoginUserDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.UserPreferencesManager;

import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler2;


/**
 * Created by bvb on 30.4.2015 г..
 */
public class SignalRTrackingService extends Service {

    private HubConnection connection;
    private Intent broadcastIntent;
    private HubProxy proxy;
    private boolean reportLocationEnabled = false;
    Location taxiLocation;
    Location myLocation;
    boolean notificationHasBeenSent = false;
    private static final String TAG = "TRACKING_SERVICE";

    int orderId;
    @Override
    public void onCreate() {
        super.onCreate();
        orderId = -1;
        // Register for Location Service broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.LOCATION_UPDATED);
        registerReceiver(locationReceiver, filter);
        Log.d(TAG, "STARTED");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Toast.makeText(this, getString(R.string.tracking_started), Toast.LENGTH_LONG).show();
        Log.d(TAG, "onStartCommand");
        Bundle data = intent.getExtras();
        orderId = data.getInt(Constants.ORDER_ID, -1);
        myLocation = data.getParcelable(Constants.LOCATION);
        String baseUrl = data.getString(Constants.BASE_URL_STORAGE);
        reportLocationEnabled = intent.getBooleanExtra(Constants.LOCATION_REPORT_ENABLED, false);

        if(orderId == -1){
            return -1;
        }

        String server =  baseUrl + Constants.HUB_ENDPOINT;
        Log.d(TAG, server);
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

        Log.d(TAG, "awaiting connection");
        SignalRFuture<Void> awaitConnection = connection.start();
        try {
            awaitConnection.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "invoking hub");
        //TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        //String phoneNumber = tMgr.getLine1Number();
        proxy.invoke(Constants.HUB_CONNECT, orderId);

        //Then call on() to handle the messages when they are received.
//        proxy.on("ok", new SubscriptionHandler1<String>() {
//            @Override
//            public void run(String msg) {
//                Log.d("result := ", msg);
//            }
//        }, String.class);

        Log.d(TAG, "registering callback");
        proxy.on(Constants.HUB_UPDATE_TAXI_LOCATION, new SubscriptionHandler2<Double, Double>() {
            @Override
            public void run(Double lat, Double lon) {
                Log.d(TAG, Constants.HUB_UPDATE_TAXI_LOCATION);
                taxiLocation = new Location("void");
                taxiLocation.setLatitude(lat);
                taxiLocation.setLongitude(lon);

                if (!notificationHasBeenSent) {
                    if( taxiHasArrived()) {
                        Log.d(TAG, "Sending notification");
                        Intent notify = new Intent(Constants.HUB_TAXI_HAS_ARRIVED_NOTIFY_BC);
                        sendOrderedBroadcast(notify, null);
                        notificationHasBeenSent = true;
                    }
                }

                broadcastIntent = new Intent(Constants.HUB_UPDATE_TAXI_LOCATION_BC);
                broadcastIntent.putExtra(Constants.LOCATION, taxiLocation);
                sendBroadcast(broadcastIntent);

            }
        }, Double.class, Double.class);

        proxy.on(Constants.HUB_TAXI_ASSIGNED, new SubscriptionHandler2<Integer, String>() {
            @Override
            public void run(Integer taxiId, String plate) {
                Log.d(TAG, Constants.HUB_TAXI_ASSIGNED);
                Intent notify = new Intent(Constants.HUB_TAXI_WAS_ASSIGNED_NOTIFY_BC);
                notify.putExtra(Constants.HUB_ASSIGNED_TAXI_ID, taxiId);
                notify.putExtra(Constants.HUB_ASSIGNED_TAXI_PLATE, plate);
                // for NotificationsReceiver
                sendOrderedBroadcast(notify, null);
            }
        }, Integer.class, String.class);

        proxy.on(Constants.HUB_ORDER_STATUS_CHANGED, new SubscriptionHandler1<Integer>() {
            @Override
            public void run(Integer orderId) {
                Log.d(TAG, Constants.HUB_ORDER_STATUS_CHANGED);
                broadcastIntent = new Intent(Constants.HUB_ORDER_STATUS_CHANGED_BC);
                broadcastIntent.putExtra(Constants.ORDER_ID, orderId);
                sendBroadcast(broadcastIntent);
            }
        }, Integer.class);

        Log.d(TAG, "DONE onStartCommand()");
        return Service.START_STICKY;
                //--------------------------------------------------------------------------------
    }

    private boolean taxiHasArrived(){
        if(myLocation == null){
            Log.d(TAG, "Don't know my location");
        }
        if(taxiLocation == null){
            Log.d(TAG, "Don't know taxi location");
        }
        if (myLocation != null && taxiLocation != null) {
            float distance = taxiLocation.distanceTo(myLocation);

            if (distance <= Constants.TAXI_ARRIVAL_DISTANCE_THRESHOLD) {
                Log.d(TAG, "TAXI HAS ARRIVED");
                return true;
            }
        }
        Log.d(TAG, "TAXI HAS NOT ARRIVED YET");
        return false;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        proxy.invoke(Constants.HUB_DISCONNECT);
        connection.stop();
        unregisterReceiver(locationReceiver);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The receiver for the Location Service location update broadcasts
     */
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {

                Bundle data = intent.getExtras();

                myLocation = data.getParcelable(Constants.LOCATION);

                if(!reportLocationEnabled){
                    return;
                }
                double lat = myLocation.getLatitude();
                double lon = myLocation.getLongitude();

                if ( proxy != null && orderId != -1){
                    Log.d(TAG, Constants.HUB_CLIENT_LOCATION_CHANGED);
                    proxy.invoke(Constants.HUB_CLIENT_LOCATION_CHANGED, orderId, lat, lon);
                }

            }
        }
    };


    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch(Exception e) {
                    Log.d("Error", e.toString());
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
}
