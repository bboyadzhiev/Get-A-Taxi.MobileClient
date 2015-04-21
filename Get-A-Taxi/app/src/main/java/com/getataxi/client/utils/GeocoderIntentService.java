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
public class GeocoderIntentService extends IntentService {
    private static final String TAG = "GEOCODE";
    protected ResultReceiver mReceiver;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeocoderIntentService(String name) {
        super(name);
    }

    private void deliverAddressToReceiver(int resultCode, String message, String addressTag) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        bundle.putString(Constants.ADDRESS_TAG, addressTag);
        mReceiver.send(resultCode, bundle);
    }

    private void deliverLocationToReceiver(int resultCode, Location location, String addressTag) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.RESULT_DATA_KEY, location);
        bundle.putString(Constants.ADDRESS_TAG, addressTag);
        mReceiver.send(resultCode, bundle);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        int geocodeType = intent.getIntExtra(Constants.GEOCODE_TYPE, Constants.GET_LOCATION);
        String addressTag = intent.getStringExtra(
                Constants.ADDRESS_TAG);
        if(geocodeType ==  Constants.GET_LOCATION) {
            String addressToDecode = intent.getStringExtra(
                    Constants.LOCATION_DATA_EXTRA);
            Location decodedLocation = new Location("");
            boolean success = false;
            try {

                List<Address> addresses;
                addresses = geocoder.getFromLocationName(addressToDecode, 1);
                if(addresses.size() > 0) {
                    double latitude= addresses.get(0).getLatitude();
                    double longitude= addresses.get(0).getLongitude();
                    decodedLocation.setLatitude(latitude);
                    decodedLocation.setLongitude(longitude);
                    success = true;
                }

            } catch (IOException e) {
                errorMessage = getString(R.string.service_not_available);
                Log.e(TAG, errorMessage, e);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_address_used);
                Log.e(TAG, errorMessage + ". " +
                        "Address: " + addressToDecode, illegalArgumentException);
            }

            if(success){
                deliverLocationToReceiver(Constants.SUCCESS_RESULT, decodedLocation, addressTag);
            }else{
                deliverLocationToReceiver(Constants.FAILURE_RESULT, decodedLocation, addressTag);
            }

        } else
        if(geocodeType ==  Constants.GET_ADDRESS) {
            Location location = intent.getParcelableExtra(
                    Constants.LOCATION_DATA_EXTRA);
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // In this sample, get just a single address.
                        1);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
                errorMessage = getString(R.string.service_not_available);
                Log.e(TAG, errorMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_lat_long_used);
                Log.e(TAG, errorMessage + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), illegalArgumentException);
            }
            // Handle case where no address was found.
            if (addresses == null || addresses.size()  == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = getString(R.string.no_address_found);
                    Log.e(TAG, errorMessage);
                }
                deliverAddressToReceiver(Constants.FAILURE_RESULT, errorMessage, addressTag);
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.i(TAG, getString(R.string.address_found));
                deliverAddressToReceiver(Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"),
                                addressFragments), addressTag);
            }
        }
    }
}


