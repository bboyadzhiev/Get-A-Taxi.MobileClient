package com.getataxi.client.comm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.UserPreferencesManager;

import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;

/**
 * Created by bvb on 30.4.2015 г..
 */
public class SignalRNotificationService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Toast.makeText(this, "Service Start", Toast.LENGTH_LONG).show();

        String server = Constants.BASE_URL;
        HubConnection connection = new HubConnection(server);
        HubProxy proxy = connection.createHubProxy("clientsHub");

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

        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tMgr.getLine1Number();
        proxy.invoke("Open", phoneNumber);

        //Then call on() to handle the messages when they are received.
        proxy.on("ok", new SubscriptionHandler1<String>() {
            @Override
            public void run(String msg) {
                Log.d("result := ", msg);
            }
        }, String.class);

        //--------------------------------------------------------------------------------
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
