package com.getataxi.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment.SelectLocationDialogListener;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.utils.LocationService;
import com.getataxi.client.utils.UserPreferencesManager;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrderMap extends FragmentActivity implements SelectLocationDialogListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public static final String BROADCAST_ACTION = "com.getataxi.client.location.UPDATED";
    private static final String DESTINATION_DIALOG_TAG = "destinationDialog";
    private static final String START_DIALOG_TAG = "startDialog";
    private double clientLat;
    private double clientLon;
    private Context context;
    private Button confirmLocationButton;
    private AddressesInputsFragment locationsInputs;
//    private Button startAddressBtn;
//    private Button destinationAddressBtn;
    private  SelectLocationDialogFragment locationDialog;

    boolean isGPSEnabled = false;
    double latitude;
    double longitude;

    LocationManager locationManager;

    /**
     * onCreate
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_map);
        context = this;

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BROADCAST_ACTION);
//        registerReceiver(locationReceiver, filter);
//
//        // Starting location service
//        Intent intent = new Intent(OrderMap.this, LocationService.class);
//        context.startService(intent);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // checking if gps is enabled
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            // Requesting location update
            Toast.makeText(context, "Locations enabled",Toast.LENGTH_SHORT);
            locationManager.requestLocationUpdates(

                    LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(context, "Locations not enabled!",Toast.LENGTH_SHORT);
        }


        this.confirmLocationButton = (Button)findViewById(R.id.btn_confirm_location);
        Button startAddressBtn = (Button) findViewById(R.id.startAddress_btn);
        startAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChooseLocationDialog(R.id.select_start_location);
            }
        });

        Button destinationAddressBtn = (Button) findViewById(R.id.destinationAddress_btn);
        destinationAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChooseLocationDialog(R.id.select_destination_location);
            }
        });

        locationsInputs = (AddressesInputsFragment)getFragmentManager().findFragmentById(R.id.addressesInputs_fragment);
        getFragmentManager().beginTransaction().hide(locationsInputs).commit();
        setUpMapIfNeeded();
    }

    LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location loc) {
            // Getting lat and lng
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();

            // Placing marker
            MarkerOptions marker = new MarkerOptions().position(
                    new LatLng(latitude, longitude)).title("Your location is ");

            mMap.addMarker(marker);

            // Moving camera view
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude)).zoom(15).build();

            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
            // Stop using gps
            stopUsingGPS(locationListener);

            // Send GPS Data Button Click event

            // Closing location listener
            locationManager.removeUpdates(this);
            locationManager.removeUpdates(locationListener);

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * Stop GPS
     * @param loc
     */
    public void stopUsingGPS(LocationListener loc) {
        if (locationManager != null) {
            locationManager.removeUpdates(loc);
        }
    }

    // OPTIONS MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (ChooseLocationDialog(id)) return true;
        return super.onOptionsItemSelected(item);
    }

    private boolean ChooseLocationDialog(int id) {
        if (id == R.id.enter_custom_locations) {
            FragmentManager fm = this.getFragmentManager();
           // AddressesInputsFragment inputs = (AddressesInputsFragment)getFragmentManager().findFragmentById(R.id.addressesInputs_fragment);
            //if(inputs != null) {
                fm.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(locationsInputs)
                        .commit();
           // }
            return true;
        }
        if (id == R.id.select_destination_location) {
            locationDialog= new SelectLocationDialogFragment();
            FragmentManager fm = this.getFragmentManager();
            locationDialog.show(fm, DESTINATION_DIALOG_TAG);
            return true;
        }
        if (id == R.id.select_start_location) {
            locationDialog= new SelectLocationDialogFragment();
            FragmentManager fm = this.getFragmentManager();
            locationDialog.show(fm, START_DIALOG_TAG);
            return true;
        }
        return false;
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        String tag = dialog.getTag();
        if (tag == DESTINATION_DIALOG_TAG ){

        }
        if (tag == START_DIALOG_TAG ){

        }
        dialog.dismiss();
    }

    @Override
    public void onLocationSelect(DialogFragment dialog, LocationDM locationDM) {
        dialog.dismiss();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    private List<LocationDM> locationDM21s;
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
      //  mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        Log.d("ORDER_MAP", "GETTING_LOCATIONS");
        RestClientManager.getLocations(context, new Callback<List<LocationDM>>() {
            @Override
            public void success(List<LocationDM> locationDMs, Response response) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
                locationDM21s = locationDMs;
                Log.d("ORDER_MAP", "SUCCESS_GETTING_LOCATIONS");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("ORDER_MAP", "ERROR_GETTING_LOCATIONS");
            }
        });
    }





    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BROADCAST_ACTION)) {
                Toast.makeText(context, "LOCATION UPDATED",Toast.LENGTH_LONG);
                Bundle data = intent.getExtras();
                clientLat = data.getDouble("Latitude");
                clientLon = data.getDouble("Longitude");

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(clientLat, clientLon), 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(clientLat, clientLon))      // Sets the center of the map to location user
                        .zoom(17)                   // Sets the zoom
                     //   .bearing(90)                // Sets the orientation of the camera to east
                     //   .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                MarkerOptions marker = new MarkerOptions().position(
                        new LatLng(clientLat, clientLon)).title("Your location: ");

                mMap.addMarker(marker);

                confirmLocationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    };

}
