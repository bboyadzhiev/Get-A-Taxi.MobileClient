package com.getataxi.client.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.getataxi.client.OrderMap;
import com.getataxi.client.R;


/**
 * Created by bvb on 30.5.2015
 */
public class NotificationsReceiver extends BroadcastReceiver {
    private static final int NOTIFY_ME_ID=1338;
    // receive peer location changed
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager mgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getResources().getString(R.string.taxi_has_arrived);
        String subtitle = context.getResources().getString(R.string.see_map);
        Notification note = new Notification(R.drawable.taxi,
                title,
                System.currentTimeMillis());
        PendingIntent i = PendingIntent.getActivity(context, 0,
                new Intent(context, OrderMap.class),
                0);

        note.setLatestEventInfo(context, title,
                subtitle,
                i);

        mgr.notify(NOTIFY_ME_ID, note);

    }

}
