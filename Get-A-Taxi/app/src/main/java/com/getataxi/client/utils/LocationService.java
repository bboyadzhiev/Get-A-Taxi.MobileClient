package com.getataxi.client.utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;

/**
 * Created by bvb on 5.4.2015 г..
 */
public class LocationService extends Service
{

    public LocationManager locationManager;
    public ClientLocationListener listener;
    public Location previousBestLocation = null;
    double latitude;
    double longitude;

    Intent intent;
    int counter = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        intent = new Intent(Constants.LOCATION_UPDATED);
    }

    @Override
    public void onStart(Intent intent, int startId)
    {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new ClientLocationListener();
       // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > Constants.LOCATION_TIMEOUT;
        boolean isSignificantlyOlder = timeDelta < -Constants.LOCATION_TIMEOUT;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        locationManager.removeUpdates(listener);
    }

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

    public class ClientLocationListener implements LocationListener
    {
        public void onLocationChanged(final Location loc)
        {
            if(isBetterLocation(loc, previousBestLocation)) {

                // Getting latitude and longitude
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
                intent.putExtra(Constants.LATITUDE, latitude);
                intent.putExtra(Constants.LONGITUDE, longitude);

                // Notify all interested parties
                sendBroadcast(intent);
            }
        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "GPS disabled!", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "GPS enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }
}
