package com.getataxi.client.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.getataxi.client.OrderMap;
import com.getataxi.client.R;


/**
 * Created by bvb on 30.5.2015
 */
public class NotificationsReceiver extends BroadcastReceiver {
    private static final int TAXI_ASSIGNMENT_ID =1338;
    private static final int TAXI_ARRIVED_ID =1339;
    // receive peer location changed
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("NOTIFICATIONS_RECEIVER", "SOMEBODY CALLED ME!" );
        NotificationManager mgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String action = intent.getAction();

        if(action.equals(Constants.HUB_TAXI_WAS_ASSIGNED_NOTIFY_BC)){
            Log.d("NOTIFICATIONS_RECEIVER", Constants.HUB_TAXI_WAS_ASSIGNED_NOTIFY_BC);
            String title = context.getResources().getString(R.string.taxi_was_assigned);
            String subtitle = intent.getStringExtra(Constants.HUB_ASSIGNED_TAXI_PLATE);
            Notification note = new Notification(R.drawable.taxi,
                    title,
                    System.currentTimeMillis());
            PendingIntent i = PendingIntent.getActivity(context, 0,
                    new Intent(context, OrderMap.class),
                    0);

            note.defaults |= Notification.DEFAULT_SOUND;
            note.defaults |= Notification.DEFAULT_VIBRATE;
            note.setLatestEventInfo(context, title, subtitle, i);

            mgr.notify(TAXI_ASSIGNMENT_ID, note);
        }

        if(action.equals(Constants.HUB_TAXI_HAS_ARRIVED_NOTIFY_BC)) {
            Log.d("NOTIFICATIONS_RECEIVER", Constants.HUB_TAXI_HAS_ARRIVED_NOTIFY_BC);
            String title = context.getResources().getString(R.string.taxi_has_arrived);
            String subtitle = context.getResources().getString(R.string.see_map);
            Notification note = new Notification(R.drawable.taxi,
                    title,
                    System.currentTimeMillis());
            Intent notifyIntent = new Intent(context, OrderMap.class);
            PendingIntent i = PendingIntent.getActivity(context, 0,
                    notifyIntent,
                    0);

            note.sound = Uri.parse("android.resource://"
                    + context.getPackageName() + "/" + R.raw.taxi_arrived);
            note.defaults |= Notification.DEFAULT_VIBRATE;

            note.setLatestEventInfo(context, title, subtitle, i);

            mgr.notify(TAXI_ARRIVED_ID, note);
        }

    }

}
