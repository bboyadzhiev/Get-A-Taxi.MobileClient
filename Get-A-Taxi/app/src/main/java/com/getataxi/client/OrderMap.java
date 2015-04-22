package com.getataxi.client;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment.SelectLocationDialogListener;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.GeocodeIntentService;
import com.getataxi.client.utils.LocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrderMap extends FragmentActivity implements SelectLocationDialogListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private String destinationDialogTag;
    private String startDialogTag;

    private double clientLat;
    private double clientLon;
    private Context context;
    private Button confirmLocationButton;

    // Addresses inputs
    private AddressesInputsFragment locationsInputs;
    private EditText startAddressEditText;
    private EditText destinationAddressEditText;
    private Button startAddressButton;
    private Button destinationAddressButton;

    private  SelectLocationDialogFragment locationDialog;

    private RelativeLayout startGroup;
    private RelativeLayout destinationGroup;

    LocationManager locationManager;

    protected Location locationToDecode;
    private GeocodeResultReceiver mResultReceiver;
    private Marker currentPositionMarker;


    /**
     * onCreate
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_map);
        context = this;

        // Start location service
        Intent intent = new Intent(OrderMap.this, LocationService.class);
        context.startService(intent);

        // Register for Location Service broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.LOCATION_UPDATED);
        registerReceiver(locationReceiver, filter);

        this.confirmLocationButton = (Button)findViewById(R.id.btn_confirm_location);
        confirmLocationButton.setEnabled(false);

        // Addresses inputs
        this.startAddressEditText = (EditText) findViewById(R.id.startAddress);
        this.destinationAddressEditText = (EditText) findViewById(R.id.destinationAddress);
        this.startAddressButton = (Button) findViewById(R.id.startAddress_btn);
        this.destinationAddressButton = (Button) findViewById(R.id.destinationAddress_btn);

        startAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // ChooseLocationDialog(R.id.select_start_location);
                String location = startAddressEditText.getText().toString();
                if (location != null && !location.equals("")) {

                }
            }
        });

        destinationAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String destinationAddress = destinationAddressEditText.getText().toString();
                if(!destinationAddress.isEmpty()){
                    startAddressGeocode(destinationDialogTag, destinationAddress);
                }
            }
        });

        locationsInputs = (AddressesInputsFragment)getFragmentManager().findFragmentById(R.id.addressesInputs_fragment);
        getFragmentManager().beginTransaction().hide(locationsInputs).commit();

        startGroup = (RelativeLayout) this.findViewById(R.id.startGroup);
        startGroup.setVisibility(View.INVISIBLE);
        destinationGroup = (RelativeLayout)this.findViewById(R.id.destinationGroup);
        destinationGroup.setVisibility(View.INVISIBLE);

        setUpMapIfNeeded();
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
            fm.beginTransaction()
                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                    .show(locationsInputs)
                    .commit();
            return true;
        }
        if (id == R.id.select_destination_location) {
            locationDialog= new SelectLocationDialogFragment();
            destinationDialogTag = locationDialog.getTag();
            FragmentManager fm = this.getFragmentManager();
            locationDialog.show(fm, destinationDialogTag);
            return true;
        }
        if (id == R.id.select_start_location) {
            locationDialog= new SelectLocationDialogFragment();
            startDialogTag = locationDialog.getTag();
            FragmentManager fm = this.getFragmentManager();
            locationDialog.show(fm, startDialogTag);
            return true;
        }
        return false;
    }

    /**
     * Custom locations dialog
     * Gets location data from user favorite locations
     * @param dialog
     */
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        String tag = dialog.getTag();
        if (tag == destinationDialogTag){

        }
        if (tag == startDialogTag){

        }
        dialog.dismiss();
    }

    @Override
    public void onLocationSelect(DialogFragment dialog, LocationDM locationDM) {
        // Location has been selected
        String tag = dialog.getTag();
        if (tag == destinationDialogTag){

        }
        if (tag == startDialogTag){

        }
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
              //  mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
                locationDM21s = locationDMs;
                Log.d("ORDER_MAP", "SUCCESS_GETTING_LOCATIONS");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("ORDER_MAP", "ERROR_GETTING_LOCATIONS");
            }
        });
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }


    /**
     * The receiver for the Location Service location update broadcasts
     */

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {
                Toast.makeText(context, "LOCATION UPDATED",Toast.LENGTH_LONG);
                Bundle data = intent.getExtras();
                clientLat = data.getDouble(Constants.LATITUDE);
                clientLon = data.getDouble(Constants.LONGITUDE);

                LatLng currentPosition =  new LatLng(clientLat, clientLon);


                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 13));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(currentPosition)      // Sets the center of the map to location user
                        .zoom(17)                   // Sets the zoom
                     //   .bearing(90)                // Sets the orientation of the camera to east
                     //   .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                if (currentPositionMarker == null){
                    String title = getResources().getString(R.string.marker_current_location_title);
                    MarkerOptions marker = new MarkerOptions()
                            .position(new LatLng(clientLat, clientLon))
                            .title(title);

                    currentPositionMarker = mMap.addMarker(marker);
                } else {
                    animateMarker(currentPositionMarker, currentPosition, false);
                    
                }

                confirmLocationButton.setEnabled(true);
                confirmLocationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        }
    };


    // GEOCODE METHODS AND RECEIVER
    /**
     * Initiates geocode of an address (get the address's location)
     * Uses Geocode Service
     * @param addressTag Denotes the type of address -
     */
    protected void startAddressGeocode(String addressTag, String addressToGeocode ) {
        Intent intent = new Intent(this, GeocodeIntentService.class);
        intent.putExtra(Constants.GEOCODE_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.GEOCODE_TYPE, Constants.GEOCODE);
        intent.putExtra(Constants.ADDRESS_DATA_EXTRA, addressToGeocode);
        intent.putExtra(Constants.ADDRESS_TAG, addressTag);
        startService(intent);
    }

    /**
     * Initiates reverse geocode of a location (get the location's address)
     * Uses Geocode Service
     * @param location
     */
    protected void startLocationReverseGeocode(Location location) {
        Intent intent = new Intent(this, GeocodeIntentService.class);
        intent.putExtra(Constants.GEOCODE_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.GEOCODE_TYPE, Constants.REVERSE_GEOCODE);
//        Parcel p = Parcel.obtain();
//        location.writeToParcel(p, 0);
//        final byte[] b = p.marshall();      //now you've got bytes
//        p.recycle();
//        intent.putExtra(Constants.LOCATION_DATA_EXTRA, b);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }


    /**
     * Receive Geocode result from Geocode Service
     */
    class GeocodeResultReceiver extends ResultReceiver {
        public GeocodeResultReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Display the address string
            // or an error message sent from the intent service.

            String addressTag = resultData.getString(Constants.ADDRESS_TAG);
            int geocodeType = resultData.getInt(Constants.GEOCODE_TYPE);
            if (resultCode == Constants.SUCCESS_RESULT) {
                if(geocodeType == Constants.REVERSE_GEOCODE) {
                    String result = resultData.getString(Constants.ADDRESS_DATA_EXTRA);
                    if (addressTag == destinationDialogTag) {
                        destinationAddressEditText.setText(result);
                    }

                    if (addressTag == startDialogTag) {
                        startAddressEditText.setText(result);
                    }
                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.address_found, Toast.LENGTH_LONG);

                }else if(geocodeType == Constants.GEOCODE){
                    Location result = resultData.getParcelable(Constants.LOCATION_DATA_EXTRA);
                    if (addressTag == destinationDialogTag) {
                        // TODO: update destination marker
                    }

                    if (addressTag == startDialogTag) {
                        // TODO: update start marker
                    }
                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.location_found, Toast.LENGTH_LONG);
                }

            } else if (resultCode == Constants.FAILURE_RESULT){
                if(geocodeType == Constants.REVERSE_GEOCODE) {
                    Toast.makeText(context, R.string.address_not_found, Toast.LENGTH_LONG);
                } else if(geocodeType == Constants.GEOCODE){
                    Toast.makeText(context, R.string.location_not_found, Toast.LENGTH_LONG);
                }
            }

        }
    }
}
