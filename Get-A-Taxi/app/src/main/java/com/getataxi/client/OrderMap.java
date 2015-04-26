package com.getataxi.client;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getataxi.client.comm.RestClientManager;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment.SelectLocationDialogListener;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.GeocodeIntentService;
import com.getataxi.client.utils.LocationService;
import com.getataxi.client.utils.UserPreferencesManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class OrderMap extends FragmentActivity implements SelectLocationDialogListener {
    public static final String TAG = "ORDER_MAP";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private String destinationDialogTag;
    private String startDialogTag;

    private Context context;

    private Button placeOrderButton;
    // Addresses inputs
    private AddressesInputsFragment locationsInputs;

    private EditText startAddressEditText;
    private EditText destinationAddressEditText;
    private ImageButton startAddressButton;
    private ImageButton destinationAddressButton;
    private  SelectLocationDialogFragment locationDialog;

    private RelativeLayout startGroup;

    private RelativeLayout destinationGroup;

    private GeocodeResultReceiver mResultReceiver;

    private Location clientLocation;

    // Initialized by the broadcast receiver
    private LocationDM currentReverseGeocodedLocation = null;

    private Marker currentLocationMarker;
    private Marker destinationLocationMarker;
    private boolean updateLocationEnabled = true;

    private List<LocationDM> favLocations;// = new ArrayList<Location>();

    private Drawable addToLocationsDrawable;
    private Drawable searchDrawable;

    /**
     * The receiver for the Location Service location update broadcasts
     */
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {
                //Toast.makeText(context, "LOCATION UPDATED",Toast.LENGTH_LONG);
                Bundle data = intent.getExtras();

                clientLocation = data.getParcelable(Constants.LOCATION);

                // Reverse geocode for an address
                initiateReverseGeocode(clientLocation, Constants.START_TAG);

                double clientLat = clientLocation.getLatitude();
                double clientLon = clientLocation.getLongitude();
                String markerTitle;
                LatLng latLng =  new LatLng(clientLat, clientLon);

                if(currentReverseGeocodedLocation != null){
                    currentReverseGeocodedLocation.latitude = clientLat;
                    currentReverseGeocodedLocation.longitude = clientLon;
                    markerTitle = currentReverseGeocodedLocation.title;
                    if(updateLocationEnabled){
                        RestClientManager.updateClientLocation(currentReverseGeocodedLocation, context,  new Callback<LocationDM>() {
                            @Override
                            public void success(LocationDM locationDM, Response response) {
                                // Store locations to prefs
                                LocationDM updatedLocation = locationDM;
                                Log.d(TAG, "SUCCESS_UPDATING_LOCATION");
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Log.d(TAG, "ERROR_UPDATING_LOCATION");
                            }
                        });
                    }
                } else {
                    markerTitle =  getResources().getString(R.string.looking_up_location);
                }

                currentLocationMarker = updateMarker(
                        currentLocationMarker,
                        latLng,
                        markerTitle
                );

                placeOrderButton.setEnabled(true);

            }
        }
    };


    /**
     * onCreate
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_map);
        context = this;
        initInputs();

        setUpMapIfNeeded();
    }

    @Override
    protected void onStart(){
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        // Start location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        context.startService(locationService);

        // Register for Location Service broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.LOCATION_UPDATED);
        registerReceiver(locationReceiver, filter);

        mResultReceiver = new GeocodeResultReceiver(new Handler());
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Stop location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        stopService(locationService);

        unregisterReceiver(locationReceiver);
    }


    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(locationReceiver);
    }

    private void initInputs() {
        // Hide addresses inputs
        startGroup = (RelativeLayout) this.findViewById(R.id.startGroup);
        startGroup.setVisibility(View.INVISIBLE);
        destinationGroup = (RelativeLayout)this.findViewById(R.id.destinationGroup);
        destinationGroup.setVisibility(View.INVISIBLE);

        addToLocationsDrawable = getResources().getDrawable(android.R.drawable.ic_menu_myplaces);
        searchDrawable = getResources().getDrawable(android.R.drawable.ic_menu_search);

        // Addresses inputs
        startAddressEditText = (EditText) findViewById(R.id.startAddress);
        destinationAddressEditText = (EditText) findViewById(R.id.destinationAddress);

        startAddressButton = (ImageButton) findViewById(R.id.startAddress_btn);
        startAddressButton.setImageDrawable(searchDrawable);
        destinationAddressButton = (ImageButton) findViewById(R.id.destinationAddress_btn);
        destinationAddressButton.setImageDrawable(searchDrawable);

        startAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ChooseLocationDialog(R.id.select_start_location);
                String startAddress = startAddressEditText.getText().toString();
                if (!startAddress.isEmpty()) {
                    initiateGeocode(startAddress, Constants.START_TAG);
                }
            }
        });

        destinationAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String destinationAddress = destinationAddressEditText.getText().toString();
                if(!destinationAddress.isEmpty()){
                    initiateGeocode(destinationAddress, Constants.DESTINATION_TAG);
                }
            }
        });

        locationsInputs = (AddressesInputsFragment)getFragmentManager()
                .findFragmentById(R.id.addressesInputs_fragment);
        getFragmentManager().beginTransaction().hide(locationsInputs).commit();

        placeOrderButton = (Button)findViewById(R.id.btn_confirm_location);
        placeOrderButton.setEnabled(false);
        placeOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!updateLocationEnabled) {
                    // Stop location service
                    Intent stopLocationServiceIntent = new Intent(OrderMap.this, LocationService.class);
                    context.stopService(stopLocationServiceIntent);
                }

                // TODO: Send new order

            }
        });
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
        if (id == R.id.select_destination_location || id == R.id.select_start_location) {
            if(favLocations != null && !favLocations.isEmpty()) {
                Bundle bundle = new Bundle();
                Parcelable wrapped = Parcels.wrap(favLocations);
                bundle.putParcelable(Constants.USER_LOCATIONS, wrapped);
                locationDialog = new SelectLocationDialogFragment();
                FragmentManager fm = this.getFragmentManager();
                locationDialog.setArguments(bundle);

                if (id == R.id.select_destination_location) {
                    destinationDialogTag = locationDialog.getTag();
                    locationDialog.show(fm, destinationDialogTag);
                    return true;
                }
                if (id == R.id.select_start_location) {
                    startDialogTag = locationDialog.getTag();
                    locationDialog.show(fm, startDialogTag);
                    return true;
                }
            }
        } else {
            Toast.makeText(context, R.string.no_user_locations_toast, Toast.LENGTH_LONG);
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


    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
      //  mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        Log.d(TAG, "GETTING_LOCATIONS");
        RestClientManager.getLocations(context, new Callback<List<LocationDM>>() {
            @Override
            public void success(List<LocationDM> locationDMs, Response response) {
                // Store locations to prefs
                favLocations = locationDMs;
                UserPreferencesManager.storeLocations(favLocations, context);
                Log.d(TAG, "SUCCESS_GETTING_LOCATIONS");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "ERROR_GETTING_LOCATIONS");
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

    private Marker updateMarker(Marker marker, LatLng location, String title ){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)      // Sets the center of the map to location user
                .zoom(17)                   // Sets the zoom
                        //   .bearing(90)   // Sets the orientation of the camera to east
                        //   .tilt(40)       // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (marker == null){

            MarkerOptions markerOpts = new MarkerOptions()
                    .position(location)
                    .title(title);

            marker = mMap.addMarker(markerOpts);
        } else {
            marker.setTitle(title);
            animateMarker(marker, location, false);
        }
        return marker;
    }

    // GEOCODE METHODS AND RECEIVER
    /**
     * Initiates geocode of an address (get the address's location)
     * Uses Geocode Service
     * @param address The address to geocode
     * @param tag Denotes the type of address
     */
    protected void initiateGeocode(String address, int tag) {
        Intent intent = new Intent(this, GeocodeIntentService.class);
        intent.putExtra(Constants.GEOCODE_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.GEOCODE_TYPE, Constants.GEOCODE);
        intent.putExtra(Constants.ADDRESS_DATA_EXTRA, address);
        intent.putExtra(Constants.GEOCODE_TAG, tag);
        startService(intent);
    }

    /**
     * Initiates reverse geocode of a location (get the location's address)
     * Uses Geocode Service
     * @param location
     */
    protected void initiateReverseGeocode(Location location, int tag) {
        Intent intent = new Intent(this, GeocodeIntentService.class);
        intent.putExtra(Constants.GEOCODE_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.GEOCODE_TYPE, Constants.REVERSE_GEOCODE);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        intent.putExtra(Constants.GEOCODE_TAG, tag);
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

            int addressTag = resultData.getInt(Constants.GEOCODE_TAG);
            Log.d("ADDRESS_TAG", ""+addressTag);
            Log.d("ADDRESS TAG", addressTag == Constants.START_TAG ? "yes":"no");
            Log.d("ADDRESS TAG", addressTag == Constants.DESTINATION_TAG ? "yes":"no");
            int geocodeType = resultData.getInt(Constants.GEOCODE_TYPE);
            if (resultCode == Constants.SUCCESS_RESULT) {
                // A location's address was received
                if(geocodeType == Constants.REVERSE_GEOCODE) {
                    String resultAddress = resultData.getString(Constants.ADDRESS_DATA_EXTRA);

                    /**
                     * Reverse geocode of the destination's location address is not necessary as
                     * the destination is searched by the address itself
                     * Implemented for future development
                     */
                    if (addressTag == Constants.DESTINATION_TAG) {
                        destinationAddressEditText.setText(resultAddress);
                        LatLng latLng = destinationLocationMarker.getPosition();
                        updateMarker(destinationLocationMarker, latLng, resultAddress);
                    }

                    if (addressTag == Constants.START_TAG) {

                        //Update inputs
                        startAddressEditText.setText(resultAddress);
                        startAddressButton.setImageDrawable(addToLocationsDrawable);
                        destinationGroup.setVisibility(View.VISIBLE);
                        destinationAddressEditText.setVisibility(View.VISIBLE);
                        destinationAddressButton.setVisibility(View.VISIBLE);
                        //Update marker
                        LatLng latLng = currentLocationMarker.getPosition();
                        updateMarker(currentLocationMarker, latLng,resultAddress);

                        //Update model
                        if(currentReverseGeocodedLocation == null){
                            currentReverseGeocodedLocation = new LocationDM();
                            currentReverseGeocodedLocation.latitude = latLng.latitude;
                            currentReverseGeocodedLocation.longitude = latLng.longitude;
                            currentReverseGeocodedLocation.address = resultAddress;
                            String addressLines[] = resultAddress.split("\\r?\\n");
                            currentReverseGeocodedLocation.title = addressLines[0];

                            // Enable ordering input
                            placeOrderButton.setEnabled(true);

                            // Resolve address only once
                            RestClientManager.updateClientLocation(currentReverseGeocodedLocation, context, new Callback<LocationDM>() {
                                @Override
                                public void success(LocationDM locationDM, Response response) {
                                    // Store locations to prefs
                                    LocationDM updatedLocation = locationDM;

                                    Log.d(TAG, "SUCCESS_UPDATING_LOCATION");
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    Log.d(TAG, "ERROR_UPDATING_LOCATION");
                                }
                            });
                        }
                    }
                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.address_found, Toast.LENGTH_LONG);

                }else if(geocodeType == Constants.GEOCODE){
                    // Address's location was received along with complete address data
                    Address address = resultData.getParcelable(Constants.LOCATION_DATA_EXTRA);

                    ArrayList<String> addressFragments = new ArrayList<String>();

                    for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addressFragments.add(address.getAddressLine(i));
                    }

                    String resolvedAddress = TextUtils.join(
                            System.getProperty("line.separator"), addressFragments);
                    LatLng location = new LatLng(address.getLatitude(), address.getLongitude());

                    if (addressTag == Constants.DESTINATION_TAG) {
                        destinationLocationMarker =
                                updateMarker(destinationLocationMarker,location,resolvedAddress);
                        destinationAddressEditText.setText(resolvedAddress);
                    }

                    if (addressTag == Constants.START_TAG) {
                        currentLocationMarker =
                                updateMarker(currentLocationMarker,location,resolvedAddress);
                        startAddressEditText.setText(resolvedAddress);
                    }

                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.location_found, Toast.LENGTH_LONG);
                }

            } else if (resultCode == Constants.FAILURE_RESULT){
                // TODO: Review failure reactions
                if(geocodeType == Constants.REVERSE_GEOCODE) {
                    // Location's address was not found, show the necessary inputs!
                    Toast.makeText(context, R.string.address_not_found, Toast.LENGTH_LONG);
                } else if(geocodeType == Constants.GEOCODE){
                    // Address's location was not found
                    Toast.makeText(context, R.string.location_not_found, Toast.LENGTH_LONG);
                }

                if (addressTag == Constants.START_TAG){
                    startGroup.setVisibility(View.VISIBLE);
                }
                if (addressTag == Constants.DESTINATION_TAG) {
                    destinationGroup.setVisibility(View.VISIBLE);
                }
            }

        }
    }
}
