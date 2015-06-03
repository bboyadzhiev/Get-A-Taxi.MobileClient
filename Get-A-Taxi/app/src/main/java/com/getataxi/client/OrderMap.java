package com.getataxi.client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
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
import com.getataxi.client.comm.SignalRTrackingService;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment;
import com.getataxi.client.comm.dialogs.SelectLocationDialogFragment.SelectLocationDialogListener;
import com.getataxi.client.comm.models.AssignedOrderDM;
import com.getataxi.client.comm.models.ClientOrderDM;
import com.getataxi.client.comm.models.LocationDM;
import com.getataxi.client.comm.models.TaxiStandDM;
import com.getataxi.client.utils.Constants;
import com.getataxi.client.utils.GeocodeIntentService;
import com.getataxi.client.utils.LocationService;
import com.getataxi.client.utils.UserPreferencesManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpStatus;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;

public class OrderMap extends ActionBarActivity implements SelectLocationDialogListener {
    public static final String TAG = "ORDER_MAP";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private String destinationDialogTag;
    private String startDialogTag;

    private Context context;
    private  String phoneNumber;

    private Button cancelOrderButton;
    private Button placeOrderButton;

    // Addresses inputs
    private AddressesInputsFragment locationsInputs;

    private EditText startAddressEditText;
    private EditText destinationAddressEditText;
    private ImageButton startAddressButton;
    private ImageButton destinationAddressButton;
    private SelectLocationDialogFragment locationDialog;
    private View mProgressView;

    private RelativeLayout startGroup;

    private RelativeLayout destinationGroup;

    private GeocodeResultReceiver mResultReceiver;

    private Location clientLocation;
    private float accuracy;
    private Marker currentLocationMarker;
    private Marker destinationLocationMarker;
    private boolean trackingEnabled;

    // Order details
    private AssignedOrderDM orderDM;
    private boolean hasAssignedOrder = false;
    private int assignedOrderId = -1;

    private List<LocationDM> favLocations;// = new ArrayList<Location>();

    // Taxi stands
    private List<TaxiStandDM> taxiStandDMs;


    // Initialized by the broadcast receiver
    private LocationDM currentReverseGeocodedLocation = null;

    private Marker taxiLocationMarker;
    private Location taxiLocation;

    private Drawable addToLocationsDrawable;
    private Drawable searchDrawable;


    // TRACKING SERVICES
    protected void initiateTracking(int orderId){
        Intent trackingIntent = new Intent(OrderMap.this, SignalRTrackingService.class);
        trackingIntent.putExtra(Constants.BASE_URL_STORAGE, UserPreferencesManager.getBaseUrl(context));
        trackingIntent.putExtra(Constants.LOCATION_REPORT_ENABLED, trackingEnabled);
        trackingIntent.putExtra(Constants.ORDER_ID, orderId);
        startService(trackingIntent);
    }

    /**
     * The receiver for the Location Service - location update broadcasts
     * and the SignalR Notification Service - peer location change broadcasts
     */
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Constants.LOCATION_UPDATED)) {
                // Client location change
                Bundle data = intent.getExtras();

                clientLocation = data.getParcelable(Constants.LOCATION);
                accuracy = data.getFloat(Constants.LOCATION_ACCURACY, Constants.LOCATION_ACCURACY_THRESHOLD);

                if(accuracy < Constants.LOCATION_ACCURACY_THRESHOLD) {
                    // Reverse geocode for an address
                    initiateReverseGeocode(clientLocation, Constants.START_TAG);
                    if(taxiStandDMs.isEmpty()){
                        updateTaxiStands(context);
                    }

                }

                Log.d("REPORTEDACURRACY", "REPORTEDACURRACY: "+accuracy);

                if(!hasAssignedOrder){
                    toggleButton(ButtonType.Place);
                }
                double clientLat = clientLocation.getLatitude();
                double clientLon = clientLocation.getLongitude();
                String markerTitle;
                LatLng latLng =  new LatLng(clientLat, clientLon);

                if(currentReverseGeocodedLocation != null){
                    currentReverseGeocodedLocation.latitude = clientLat;
                    currentReverseGeocodedLocation.longitude = clientLon;
                    markerTitle = currentReverseGeocodedLocation.title;

                } else {
                    markerTitle =  getResources().getString(R.string.looking_up_location);
                }

                currentLocationMarker = updateMarker(
                        currentLocationMarker,
                        latLng,
                        markerTitle,
                        false
                );
                currentLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.person));



            } else if(action.equals(Constants.HUB_PEER_LOCATION_CHANGED_BC)){
                // Taxi location change

                // Checking if we have any order data
                if(orderDM == null){
                    orderDM = new AssignedOrderDM();
                    return;
                }
                if(orderDM.taxiId == -1){
                    orderDM.taxiPlate = getResources().getString(R.string.getting_details_txt);
                    // Try to get the assigned order details
                    getAssignedOrder();
                }

                Bundle data = intent.getExtras();
                taxiLocation = data.getParcelable(Constants.LOCATION);

                String markerTitle = orderDM.taxiPlate + " - " + orderDM.driverName;
                LatLng latLng =  new LatLng(taxiLocation.getLatitude(), taxiLocation.getLongitude());
                taxiLocationMarker = updateMarker(
                        taxiLocationMarker,
                        latLng,
                        markerTitle,
                        false
                );
                taxiLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.taxi));
            }
        }
    };

    private void updateTaxiStands(Context context) {
        RestClientManager.getTaxiStands(clientLocation.getLatitude(), clientLocation.getLongitude(), context, new Callback<List<TaxiStandDM>>() {
            @Override
            public void success(List<TaxiStandDM> taxiStands, Response response) {
                taxiStandDMs.clear();
                taxiStandDMs.addAll(taxiStands);
                for (TaxiStandDM stand : taxiStands) {
                    MarkerOptions markerOpts = new MarkerOptions()
                            .position(new LatLng(stand.latitude, stand.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxistand))
                            .title(stand.alias);

                    Marker marker = mMap.addMarker(markerOpts);
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }


    // ACTIVITY LIFECYCLE
    /**
     * onCreate
     * @param savedInstanceState - the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_map);
        context = this;
        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tMgr.getLine1Number();

        mProgressView = findViewById(R.id.order_map_progress);
        accuracy = Constants.LOCATION_ACCURACY_THRESHOLD;
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

        trackingEnabled = UserPreferencesManager.getTrackingState(context);
        hasAssignedOrder = UserPreferencesManager.hasAssignedOrder(context);
        if(hasAssignedOrder) {
            assignedOrderId = UserPreferencesManager.getLastOrderId(context);
            getAssignedOrder();
        }

        if(hasAssignedOrder) {
            toggleButton(ButtonType.Cancel);
        } else {
            toggleButton(ButtonType.Place);
        }

        // Start location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        locationService.putExtra(Constants.LOCATION_REPORT_TITLE, phoneNumber);
        context.startService(locationService);

        IntentFilter filter = new IntentFilter();
        // Register for Location Service broadcasts
        filter.addAction(Constants.LOCATION_UPDATED);
        // And peer location change
        filter.addAction(Constants.HUB_PEER_LOCATION_CHANGED_BC);
        registerReceiver(locationReceiver, filter);

        mResultReceiver = new GeocodeResultReceiver(new Handler());

        if(taxiStandDMs == null){
            taxiStandDMs = new ArrayList<TaxiStandDM>();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Stop location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        stopService(locationService);

        // Stop tracking service
        Intent trackingService = new Intent(OrderMap.this, SignalRTrackingService.class);
        stopService(trackingService);

        unregisterReceiver(locationReceiver);
    }


    // BUSINESS LOGIC
    private void getAssignedOrder(){

        if(!hasAssignedOrder){
            toggleButton(ButtonType.Place);
            return;
        }

        // Client was in active order status
        showProgress(true);
        RestClientManager.getOrder(assignedOrderId, context, new Callback<AssignedOrderDM>() {
            @Override
            public void success(AssignedOrderDM assignedOrderDM, Response response) {
                toggleButton(ButtonType.Cancel);
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    try {
                        orderDM = assignedOrderDM;

                        hasAssignedOrder = true;
                        initiateTracking(assignedOrderId);

                        currentLocationMarker = updateMarker(
                                currentLocationMarker,
                                new LatLng(assignedOrderDM.orderLatitude, assignedOrderDM.orderLongitude),
                                assignedOrderDM.orderAddress,
                                true
                        );
                        if (!assignedOrderDM.destinationAddress.isEmpty()) {
                            destinationLocationMarker = updateMarker(destinationLocationMarker,
                                    new LatLng(assignedOrderDM.destinationLatitude, assignedOrderDM.destinationLongitude),
                                    assignedOrderDM.destinationAddress,
                                    false
                            );
                        }


                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } finally {
                        showProgress(false);
                    }
                }

                if (status == HttpStatus.SC_NOT_FOUND) {
                    // Clear stored order id
                    clearStoredOrder();
                    showProgress(false);
                }

            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                // Clear stored order id
                clearStoredOrder();
                showProgress(false);
            }
        });
    }

    private void showToastError(RetrofitError error) {
        if(error.getResponse() != null) {
            if (error.getResponse().getBody() != null) {
                String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
                if(!json.isEmpty()){
                    Toast.makeText(context, json, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearStoredOrder() {
        UserPreferencesManager.clearOrder(context);
        hasAssignedOrder = false;
        assignedOrderId = -1;
        orderDM = null;
        toggleButton(ButtonType.Place);
    }

    private ClientOrderDM getClientOrderDM() {
        ClientOrderDM clientOrder = new ClientOrderDM();
        clientOrder.orderLatitude = currentReverseGeocodedLocation.latitude;
        clientOrder.orderLongitude = currentReverseGeocodedLocation.longitude;
        clientOrder.orderAddress = startAddressEditText.getText().toString();
        if(destinationAddressEditText.getText() != null) {
            clientOrder.destinationAddress = destinationAddressEditText.getText().toString();
            clientOrder.destinationLatitude = destinationLocationMarker.getPosition().latitude;
            clientOrder.destinationLongitude = destinationLocationMarker.getPosition().longitude;
        }
        return clientOrder;
    }

    private AssignedOrderDM fromClientOrderDM(ClientOrderDM clientOrder) {
        AssignedOrderDM order = new AssignedOrderDM();
        order.orderId = clientOrder.orderId;
        order.orderAddress = clientOrder.orderAddress;
        order.orderLatitude = clientOrder.orderLatitude;
        order.orderLongitude = clientOrder.orderLongitude;
        order.destinationAddress = clientOrder.destinationAddress;
        order.destinationLatitude = clientOrder.destinationLatitude;
        order.destinationLongitude = clientOrder.destinationLongitude;
        order.userComment = clientOrder.userComment;
        order.taxiId = -1;
        return  order;
    }

    // USER INTERFACE
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
                if (!destinationAddress.isEmpty()) {
                    initiateGeocode(destinationAddress, Constants.DESTINATION_TAG);
                }
            }
        });

        locationsInputs = (AddressesInputsFragment)getFragmentManager()
                .findFragmentById(R.id.addressesInputs_fragment);
        getFragmentManager().beginTransaction().hide(locationsInputs).commit();

        cancelOrderButton = (Button)findViewById(R.id.btn_cancel_order);
        cancelOrderButton.setEnabled(false);
        cancelOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelOrderButton.setEnabled(false);
                if (trackingEnabled) {
                    // Stop location service
                    Intent stopLocationServiceIntent = new Intent(OrderMap.this, LocationService.class);
                    context.stopService(stopLocationServiceIntent);
                }
                showProgress(true);

                if(hasAssignedOrder){
                    // Order in progress, try to cancel it
                    RestClientManager.cancelOrder(assignedOrderId, context, new Callback<ClientOrderDM>() {
                        @Override
                        public void success(ClientOrderDM clientOrderDM, Response response) {
                            showProgress(false);
                            int status = response.getStatus();
                            clearStoredOrder();
                            if (status == HttpStatus.SC_OK) {
                                // Cancelled successfully
                                Toast.makeText(context, getResources().getString(R.string.order_cancelled_toast), Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (status == HttpStatus.SC_BAD_REQUEST) {
                                // Cancelled or finished already
                                Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                                return;
                            }

                            Toast.makeText(context, getResources().getString(R.string.last_order_not_found_toast), Toast.LENGTH_LONG).show();

                        }

                        @Override
                        public void failure(RetrofitError error) {
                            showProgress(false);
                            //clearStoredOrder();
                            //placeOrderButton.setEnabled(true);
                            toggleButton(ButtonType.Cancel);
                            showToastError(error);
                        }
                    });
                }
            }
        });

        placeOrderButton = (Button)findViewById(R.id.btn_place_order);
        if(clientLocation != null && !hasAssignedOrder) {
            placeOrderButton.setEnabled(false);
        }
        placeOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeOrderButton.setEnabled(false);
                if (trackingEnabled) {
                    // Stop location service
                    Intent stopLocationServiceIntent = new Intent(OrderMap.this, LocationService.class);
                    context.stopService(stopLocationServiceIntent);
                }

                showProgress(true);

                // No order in progress, place new order
                ClientOrderDM clientOrder = getClientOrderDM();

                RestClientManager.addOrder(clientOrder, context, new Callback<ClientOrderDM>() {
                    @Override
                    public void success(ClientOrderDM clientOrder, Response response) {
                        showProgress(false);
                        int status = response.getStatus();

                            if (status == HttpStatus.SC_OK) {
                            try {
                                orderDM = fromClientOrderDM(clientOrder);
                                UserPreferencesManager.storeOrderId(clientOrder.orderId, context);
                                hasAssignedOrder = true;
                                assignedOrderId = clientOrder.orderId;
                                initiateTracking(clientOrder.orderId);
                                toggleButton(ButtonType.Cancel);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }

                        if(status == HttpStatus.SC_BAD_REQUEST){
                            Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        showProgress(false);
                        showToastError(error);
                        toggleButton(ButtonType.Place);
                    }
                });
            }
        });
    }

    public enum ButtonType {
        Place, Cancel
    }

    private void toggleButton(ButtonType button){
        if(button == ButtonType.Cancel){ // Cancel Order
            cancelOrderButton.setVisibility(View.VISIBLE);
            cancelOrderButton.setEnabled(true);
            placeOrderButton.setVisibility(View.INVISIBLE);
            placeOrderButton.setEnabled(false);
        } else if (button == ButtonType.Place) { // Place Order
            cancelOrderButton.setVisibility(View.INVISIBLE);
            cancelOrderButton.setEnabled(false);
            placeOrderButton.setVisibility(View.VISIBLE);

            if(accuracy < Constants.LOCATION_ACCURACY_THRESHOLD) {
                placeOrderButton.setEnabled(true);
            } else {
                placeOrderButton.setEnabled(false);
            }


        }
    }

    /**
     * Shows the ordering progress UI
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // OPTIONS MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
        if(UserPreferencesManager.getTrackingState(context)){
            menu.findItem(R.id.tracking_location_cb).setChecked(true);
        } else {
            menu.findItem(R.id.tracking_location_cb).setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (ChooseLocationDialog(id)) return true;

        if(id == R.id.tracking_location_cb){
            item.setChecked(!item.isChecked());
            trackingEnabled = item.isChecked();
            if(trackingEnabled) {
                Toast.makeText(context, R.string.tracking_enabled_txt, Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, R.string.tracking_disabled_txt, Toast.LENGTH_LONG).show();
            }


            UserPreferencesManager.setTrackingState(trackingEnabled, context);
            return true;
        }

        if(id == R.id.order_map_action_exit) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    // LOCATIONS DIALOG
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

                startDialogTag = locationDialog.getTag();
                locationDialog.show(fm, startDialogTag);
                return true;
            } else {
                Toast.makeText(context, R.string.no_user_locations_toast, Toast.LENGTH_LONG).show();
            }
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
        if (tag.equals(destinationDialogTag)){

        }
        if (tag.equals(startDialogTag)){

        }
        dialog.dismiss();
    }

    @Override
    public void onLocationSelect(DialogFragment dialog, LocationDM locationDM) {
        // Location has been selected
        String tag = dialog.getTag();
        if (tag.equals(destinationDialogTag)){

        }
        if (tag.equals(startDialogTag)){

        }
        dialog.dismiss();

    }

    // GOOGLE MAP AND MARKERS
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
                showToastError(error);
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

    private Marker updateMarker(Marker marker, LatLng location, String title, boolean animateEnabled ){
        if(animateEnabled) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(location)      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                            //   .bearing(90)   // Sets the orientation of the camera to east
                            //   .tilt(40)       // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        if (marker == null){

            MarkerOptions markerOpts = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.person))
                    .title(title);

            marker = mMap.addMarker(markerOpts);
            animateMarker(marker, location, false);
        } else {
            marker.setTitle(title);
            animateMarker(marker, location, false);
        }
        marker.showInfoWindow();
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
     * @param location the location whose address will be resolved
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
                        updateMarker(destinationLocationMarker, latLng, resultAddress, true);
                    }

                    if (addressTag == Constants.START_TAG) {

                        //Update inputs
                        startAddressEditText.setText(resultAddress);
                        startAddressButton.setImageDrawable(addToLocationsDrawable);
                        destinationGroup.setVisibility(View.VISIBLE);
                        destinationAddressEditText.setVisibility(View.VISIBLE);
                        destinationAddressButton.setVisibility(View.VISIBLE);
                        FragmentManager fm = getFragmentManager();
                        fm.beginTransaction()
                                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                .show(locationsInputs)
                                .commitAllowingStateLoss();

                        //Update marker
                        LatLng latLng = currentLocationMarker.getPosition();
                        updateMarker(currentLocationMarker, latLng,resultAddress, true);

                        //Update model
                        if(currentReverseGeocodedLocation == null){
                            currentReverseGeocodedLocation = new LocationDM();
                            currentReverseGeocodedLocation.latitude = latLng.latitude;
                            currentReverseGeocodedLocation.longitude = latLng.longitude;
                            currentReverseGeocodedLocation.address = resultAddress;
                            String addressLines[] = resultAddress.split("\\r?\\n");
                            currentReverseGeocodedLocation.title = addressLines[0];

                            // Enable ordering input
                            toggleButton(ButtonType.Place);

                            // Resolve address only once
//                            if(trackingEnabled) {
//                                // Update client's location in the system
//                                RestClientManager.updateClientLocation(currentReverseGeocodedLocation, context, new Callback<LocationDM>() {
//                                    @Override
//                                    public void success(LocationDM locationDM, Response response) {
//                                        int status = response.getStatus();
//                                        clearStoredOrder();
//                                        if (status == HttpStatus.SC_OK) {
//                                            LocationDM updatedLocation = locationDM;
//                                        }
//                                        Log.d(TAG, "SUCCESS_UPDATING_LOCATION");
//                                    }
//
//                                    @Override
//                                    public void failure(RetrofitError error) {
//                                        Log.d(TAG, "ERROR_UPDATING_LOCATION");
//                                        showToastError(error);
//                                    }
//                                });
//                            }
                        }
                    }
                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.address_found, Toast.LENGTH_LONG).show();

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
                                updateMarker(destinationLocationMarker,location,resolvedAddress, true);
                        destinationAddressEditText.setText(resolvedAddress);
                        destinationLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.destination));
                    }

                    if (addressTag == Constants.START_TAG) {
                        currentLocationMarker =
                                updateMarker(currentLocationMarker,location,resolvedAddress, true);
                        startAddressEditText.setText(resolvedAddress);
                    }

                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.location_found, Toast.LENGTH_LONG).show();
                }

            } else if (resultCode == Constants.FAILURE_RESULT){
                if(geocodeType == Constants.REVERSE_GEOCODE) {
                    // Location's address was not found, show the necessary inputs!
                    Toast.makeText(context, R.string.address_not_found, Toast.LENGTH_LONG).show();
                } else if(geocodeType == Constants.GEOCODE){
                    // Address's location was not found
                    Toast.makeText(context, R.string.location_not_found, Toast.LENGTH_LONG).show();
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
