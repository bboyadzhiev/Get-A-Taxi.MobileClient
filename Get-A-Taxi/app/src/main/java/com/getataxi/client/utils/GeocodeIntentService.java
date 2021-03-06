package com.getataxi.client.utils;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.getataxi.client.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by bvb on 21.4.2015 г..
 */
public class GeocodeIntentService extends IntentService {
    private static final String TAG = "GEOCODE_SERVICE";
    protected ResultReceiver mReceiver;

    public GeocodeIntentService() {
        super("GeocodeIntentService");
    }
    private void deliverAddressToReceiver(int resultCode, String address, int addressTag) {
        Bundle bundle = new Bundle();
        //ADD GEOCODE TYPE
        bundle.putInt(Constants.GEOCODE_TYPE, Constants.REVERSE_GEOCODE);
        bundle.putString(Constants.ADDRESS_DATA_EXTRA, address);
        bundle.putInt(Constants.GEOCODE_TAG, addressTag);
        Log.d(TAG, "SUCCESS_REVERSE_GEOCODE");
        Log.d("RESULT_CODE", "" + resultCode );
        Log.d("ADDRESS_DATA_EXTRA", address);
        Log.d("GEOCODE_TAG", " " + addressTag);
        mReceiver.send(resultCode, bundle);
    }

    private void deliverLocationToReceiver(int resultCode, Address geocodedAddress, int addressTag) {

        if ( geocodedAddress != null) {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.GEOCODE_TYPE, Constants.GEOCODE);
            bundle.putParcelable(Constants.LOCATION_DATA_EXTRA, geocodedAddress);
            bundle.putInt(Constants.GEOCODE_TAG, addressTag);
            mReceiver.send(resultCode, bundle);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mReceiver = intent.getParcelableExtra(Constants.GEOCODE_RECEIVER);

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }

        String errorMessage = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        int geocodeType = intent.getIntExtra(Constants.GEOCODE_TYPE, Constants.REVERSE_GEOCODE);
        int tag = intent.getIntExtra(Constants.GEOCODE_TAG, Constants.START_TAG);
        if(geocodeType ==  Constants.GEOCODE) {
            String addressToGeocode = intent.getStringExtra(Constants.ADDRESS_DATA_EXTRA);
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName(addressToGeocode, 1);
            } catch (IOException e) {
                errorMessage = getString(R.string.service_not_available);
                Log.e(TAG, errorMessage, e);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_address_used);
                Log.e(TAG, errorMessage + ": " + addressToGeocode, illegalArgumentException);
            }

            if(addresses != null && addresses.size() > 0) {
                Address geocodedAddress = addresses.get(0);
                deliverLocationToReceiver(Constants.SUCCESS_RESULT, geocodedAddress, tag);
            } else {
                deliverLocationToReceiver(Constants.FAILURE_RESULT, null, tag);
            }

        } else if(geocodeType ==  Constants.REVERSE_GEOCODE) {
            Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // Get just a single address.
                        1);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
                errorMessage = getString(R.string.service_not_available);
                Log.e(TAG, errorMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_lat_long_used) +" " +
                        "Lat: " + location.getLatitude() +
                        ", Lon: " + location.getLongitude();
                Log.e(TAG, errorMessage ,
                        illegalArgumentException);
            }
            // Handle case where no address was found.
            if (addresses == null || addresses.size()  == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = getString(R.string.address_not_found);
                    Log.e(TAG, errorMessage);
                }
                deliverAddressToReceiver(Constants.FAILURE_RESULT, errorMessage, tag);
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.i(TAG, getString(R.string.address_found));
                deliverAddressToReceiver(
                        Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"),addressFragments),
                        tag);
            }
        }
    }
}


