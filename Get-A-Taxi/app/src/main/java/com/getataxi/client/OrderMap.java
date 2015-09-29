package com.getataxi.client;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.getataxi.client.comm.models.OrderDetailsDM;
import com.getataxi.client.comm.models.OrderDM;
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
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
    private float lastZoom = Constants.MAP_ANIMATION_ZOOM;

    private String destinationDialogTag;
    private String startDialogTag;

    private Context context;
    //private  String phoneNumber;

    private Button cancelOrderButton;
    private Button placeOrderButton;

    // Addresses inputs
    private AddressesInputsFragment locationsInputs;

    private EditText startAddressEditText;
    private ImageButton startAddressButton;
    private ImageButton startFavoriteButton;

    private EditText destinationAddressEditText;
    private ImageButton destinationAddressButton;
    private ImageButton destinationFavoriteButton;

    private EditText commentEditText;

    private SelectLocationDialogFragment locationDialog;
    private View mProgressView;

    private RelativeLayout startGroup;

    private RelativeLayout destinationGroup;
    private RelativeLayout commentGroup;

    private GeocodeResultReceiver mResultReceiver;

    private Location clientLocation;
    private Location clientUpdatedLocation;
    private Location lastReverseGeocodedLocation;
    private String currentAddress;
    private float accuracy;
    private Marker clientLocationMarker;

    private Location destinationLocation;
    private String destinationAddress;
    private Marker destinationLocationMarker;
    private boolean trackingEnabled;

    // Order details
    private OrderDetailsDM placedOrderDetailsDM;
    private boolean inActiveOrder = false;
    private int activeOrderId = -1;

    private List<LocationDM> favLocations;// = new ArrayList<Location>();

    // Taxi stands
    private List<TaxiStandDM> taxiStandDMs;


    // Initialized by the broadcast receiver
    //private LocationDM lastReverseGeocodedLocation = null;

    private Marker taxiLocationMarker;
    private Location taxiLocation;

    private Drawable addToLocationsDrawable;
    private Drawable searchDrawable;


    // TRACKING SERVICES
    protected void initiateTracking(int orderId){
        Intent trackingIntent = new Intent(OrderMap.this, SignalRTrackingService.class);
        trackingIntent.putExtra(Constants.BASE_URL_STORAGE, UserPreferencesManager.getBaseUrl(context));
        trackingIntent.putExtra(Constants.LOCATION_REPORT_ENABLED, trackingEnabled);
        trackingIntent.putExtra(Constants.LOCATION, clientLocation);
        trackingIntent.putExtra(Constants.ORDER_ID, orderId);
        startService(trackingIntent);
    }

    private void stopTrackingService() {
        // Stop tracking service
        Intent trackingService = new Intent(OrderMap.this, SignalRTrackingService.class);
        stopService(trackingService);
    }

    //Location Service
    private void stopLocationService() {
            // Stop location service
            Intent stopLocationServiceIntent = new Intent(OrderMap.this, LocationService.class);
            context.stopService(stopLocationServiceIntent);
    }

    // BROADCAST RECEIVERS
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

                Log.d("ORDER_MAP", "LOCATION_UPDATED");
                clientUpdatedLocation = data.getParcelable(Constants.LOCATION);
                clientLocation = clientUpdatedLocation;
                accuracy = data.getFloat(Constants.LOCATION_ACCURACY, Constants.LOCATION_ACCURACY_THRESHOLD);

                if(accuracy < Constants.LOCATION_ACCURACY_THRESHOLD) {
                    if(!inActiveOrder) {
                        // Reverse geocode for an address, only if not in active order and client location has changed dramatically
                        if(lastReverseGeocodedLocation == null || lastReverseGeocodedLocation.distanceTo(clientLocation) >= Constants.REVERSE_GEOCODE_DISTANCE_THRESHOLD) {
                            initiateReverseGeocode(clientLocation, Constants.START_TAG);
                        }

                        if(taxiStandDMs.isEmpty()){
                            updateTaxiStands(context);
                        }
                    }
                }

                Log.d("ORDER_MAP", "REPORTEDACURRACY: "+accuracy);

                // Allow ordering
                if(!inActiveOrder){
                    toggleButton(ButtonType.Place);
                }

//                double clientLat = clientLocation.getLatitude();
//                double clientLon = clientLocation.getLongitude();
                String currentAddressMarkerTitle;
//                if(lastReverseGeocodedLocation != null){
//                    lastReverseGeocodedLocation.latitude = clientLat;
//                    lastReverseGeocodedLocation.longitude = clientLon;
//                    currentAddressMarkerTitle = lastReverseGeocodedLocation.title;
//
//                } else {
//                    currentAddressMarkerTitle =  getResources().getString(R.string.looking_up_location);
//                }
                if(currentAddress !=null && !currentAddress.isEmpty()){
                    currentAddressMarkerTitle = currentAddress;
                } else {
                    currentAddressMarkerTitle =  getResources().getString(R.string.looking_up_location);
                }

                updateClientMarkers(currentAddressMarkerTitle, true, false);

            } else if(action.equals(Constants.HUB_UPDATE_TAXI_LOCATION_BC)){
                // Taxi location change

                // Checking if we have any order data
                if(placedOrderDetailsDM == null){
                    placedOrderDetailsDM = new OrderDetailsDM();
                    placedOrderDetailsDM.taxiId = -1;
                }
                String markerTitle;
                Bundle data = intent.getExtras();
                taxiLocation = data.getParcelable(Constants.LOCATION);
                LatLng latLng =  new LatLng(taxiLocation.getLatitude(), taxiLocation.getLongitude());

                if(placedOrderDetailsDM.taxiId == -1){
                    // Don't know the taxi details yet
                    markerTitle = getResources().getString(R.string.getting_details_txt);
                    // Try to get the assigned order details
                    updateActiveOrderDetails();
                    taxiLocationMarker = updateMarker(
                            taxiLocationMarker,
                            latLng,
                            markerTitle,
                            R.drawable.taxi,
                            false, false
                    );
                } else {
                    // Taxi details available, updating marker
                    updateTaxiMarkerDetailsFromOrder(latLng);
                }



            } else if(action.equals(Constants.HUB_TAXI_WAS_ASSIGNED_NOTIFY_BC)){
                // Order status has been changed by the taxi driver
                int taxiId = intent.getIntExtra(Constants.HUB_ASSIGNED_TAXI_ID, -1);
                if(taxiId != -1){
                    updateActiveOrderDetails();
                }

            } else if(action.equals(Constants.HUB_ORDER_STATUS_CHANGED_BC)){
                int orderId = intent.getIntExtra(Constants.ORDER_ID, -1);
                if(orderId != -1 && activeOrderId == orderId){
                    updateActiveOrderDetails();
                }
            }
        }
    };

    private void updateTaxiMarkerDetailsFromOrder(LatLng latLng) {
        String markerTitle;
        markerTitle = placedOrderDetailsDM.taxiPlate + " - " + placedOrderDetailsDM.driverName;
        if(placedOrderDetailsDM.status == Constants.OrderStatus.Waiting.getValue() || placedOrderDetailsDM.status == Constants.OrderStatus.InProgress.getValue()) {
            taxiLocationMarker = updateMarker(
                    taxiLocationMarker,
                    latLng,
                    markerTitle,
                    R.drawable.taxi,
                    true, false
            );
        }
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
        //TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        //phoneNumber = tMgr.getLine1Number();
        mProgressView = findViewById(R.id.order_map_progress);
        accuracy = Constants.LOCATION_ACCURACY_THRESHOLD;
        initInputs();

        // Start location service
        Intent locationService = new Intent(OrderMap.this, LocationService.class);
        //locationService.putExtra(Constants.LOCATION_REPORT_TITLE, phoneNumber);
        context.startService(locationService);

        trackingEnabled = UserPreferencesManager.getTrackingState(context);
        inActiveOrder = UserPreferencesManager.hasActiveOrder(context);
        if(inActiveOrder) {
            activeOrderId = UserPreferencesManager.getLastOrderId(context);
            updateUnfinishedOrderDetails();
            toggleButton(ButtonType.Cancel);
        } else {
            toggleButton(ButtonType.Place);
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        // Cancel all notifications
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                .cancelAll();

        IntentFilter filter = new IntentFilter();
        // Register for Location Service broadcasts
        filter.addAction(Constants.LOCATION_UPDATED);
        // And peer location change
        filter.addAction(Constants.HUB_UPDATE_TAXI_LOCATION_BC);
        filter.addAction(Constants.HUB_ORDER_STATUS_CHANGED_BC);
        filter.addAction(Constants.HUB_TAXI_WAS_ASSIGNED_NOTIFY_BC);
        registerReceiver(locationReceiver, filter);

        mResultReceiver = new GeocodeResultReceiver(new Handler());

        if(taxiStandDMs == null){
            taxiStandDMs = new ArrayList<>();
        }

        favLocations = UserPreferencesManager.loadLocations(context);
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
        stopLocationService();

        // Stop tracking service
        stopTrackingService();

        if(!inActiveOrder){
            logoutFromSystem();
        }

        unregisterReceiver(locationReceiver);
    }

    private void logoutFromSystem(){
        RestClientManager.logout(context,new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                int status  = response.getStatus();
                if (status == HttpStatus.SC_OK){
                    UserPreferencesManager.logoutUser(context);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
            }
        });
    }

    // BUSINESS LOGIC
    private void updateActiveOrderDetails(){
        showProgress(true);
        RestClientManager.getOrder(activeOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM existingOrderDM, Response response) {
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    try {
                        if (existingOrderDM.status == Constants.OrderStatus.Finished.getValue()
                                || existingOrderDM.status == Constants.OrderStatus.Cancelled.getValue()) {
                            //cleanup order details
                            clientUpdatedLocation = null;
                            if (taxiLocation != null) clientLocation = taxiLocation;
                            clearStoredOrder();
                            //removeAddressesInputs();
                            //destinationGroup.setVisibility(View.VISIBLE);
                            String markerTitle = getResources().getString(R.string.looking_up_location);
                            // Restore client marker
                            clientLocationMarker = null;
                            clientLocationMarker = updateMarker(
                                    clientLocationMarker,
                                    new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude()),
                                    markerTitle,
                                    R.drawable.person,
                                    true,
                                    false);

                        }

                        if (existingOrderDM.status == Constants.OrderStatus.InProgress.getValue()) {
                            updatePlacedOrderDM(existingOrderDM);
                            //removeAddressesInputs();
                            // Removing current client marker, it should be the taxi
                            if (clientLocationMarker != null) {
                                clientLocationMarker.remove();
                            }
                            cancelOrderButton.setEnabled(false);
                        }

                        if (existingOrderDM.status == Constants.OrderStatus.Unassigned.getValue()
                                || existingOrderDM.status == Constants.OrderStatus.Waiting.getValue()) {
                            updatePlacedOrderDM(existingOrderDM);


                            if (existingOrderDM.status == Constants.OrderStatus.Unassigned.getValue()) {
                                cleanTaxiMarkerAndLocations();
                            } else if (taxiLocation != null) {
                                LatLng latLng = new LatLng(taxiLocation.getLatitude(), taxiLocation.getLongitude());
                                updateTaxiMarkerDetailsFromOrder(latLng);
                            }

                            updateClientMarkers("Ordered at:" + existingOrderDM.orderAddress, true, false);
                        }

                        updateOrderInputs(existingOrderDM);

                        invalidateOptionsMenu();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } finally {
                        showProgress(false);
                    }
                }

                if (status == HttpStatus.SC_NOT_FOUND) {
                    clearStoredOrder();
                    showProgress(false);
                }

            }

            @Override
            public void failure(RetrofitError error) {
                showToastError(error);
                showProgress(false);
            }
        });
    }

    private void updateUnfinishedOrderDetails(){
        // Client was in active order status
        showProgress(true);
        RestClientManager.getOrder(activeOrderId, context, new Callback<OrderDetailsDM>() {
            @Override
            public void success(OrderDetailsDM existingOrderDM, Response response) {
                toggleButton(ButtonType.Cancel);
                int status = response.getStatus();
                if (status == HttpStatus.SC_OK) {
                    try {

                        if (existingOrderDM.status == Constants.OrderStatus.Finished.getValue()
                                || existingOrderDM.status == Constants.OrderStatus.Cancelled.getValue()) {
                            clearStoredOrder();
                            //removeAddressesInputs();
                            //destinationGroup.setVisibility(View.VISIBLE);
                        }

                        if (existingOrderDM.status == Constants.OrderStatus.InProgress.getValue()) {
                            updatePlacedOrderDM(existingOrderDM);
                            updateClientMarkers("Ordered at:" + existingOrderDM.orderAddress, true, true);
                            initiateTracking(activeOrderId);
                            //removeAddressesInputs();
                            // Removing current client marker, it should be the taxi
                            if (clientLocationMarker != null) {
                                clientLocationMarker.remove();
                            }
                            cancelOrderButton.setEnabled(false);
                        }

                        if (existingOrderDM.status == Constants.OrderStatus.Unassigned.getValue()
                                || existingOrderDM.status == Constants.OrderStatus.Waiting.getValue()) {

                            if (existingOrderDM.status == Constants.OrderStatus.Unassigned.getValue()) {
                                cleanTaxiMarkerAndLocations();
                            }
                            //destinationGroup.setVisibility(View.VISIBLE);
                            updatePlacedOrderDM(existingOrderDM);
                            updateClientMarkers("Ordered at:" + existingOrderDM.orderAddress, true, true);
                            initiateTracking(activeOrderId);
                        }

                        updateOrderInputs(existingOrderDM);
                        invalidateOptionsMenu();

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

    private void updatePlacedOrderDM(OrderDetailsDM existingOrderDM) {
        placedOrderDetailsDM = existingOrderDM;
            UserPreferencesManager.storeOrderId(existingOrderDM.orderId, context);
            activeOrderId = existingOrderDM.orderId;
            inActiveOrder = true;


        currentAddress = placedOrderDetailsDM.orderAddress;
        if(clientUpdatedLocation == null) {
            clientLocation = new Location("fromServer");
            clientLocation.setLatitude(placedOrderDetailsDM.orderLatitude);
            clientLocation.setLongitude(placedOrderDetailsDM.orderLongitude);
        }

        if(placedOrderDetailsDM.destinationAddress != null){
            destinationAddress = placedOrderDetailsDM.destinationAddress;
        }
    }

    private OrderDM prepareClientOrderDM() {
        OrderDM clientOrder = new OrderDM();
        clientOrder.orderAddress = currentAddress;
        clientOrder.orderLatitude = clientLocation.getLatitude();
        clientOrder.orderLongitude = clientLocation.getLongitude();

        if(destinationAddress != null && !destinationAddress.isEmpty()) {
            clientOrder.destinationAddress = destinationAddress;
            if(destinationLocation != null) {
                clientOrder.destinationLatitude = destinationLocation.getLatitude();
                clientOrder.destinationLongitude = destinationLocation.getLongitude();
            }
        }

        String clientComment = commentEditText.getText().toString();
        if(!clientComment.isEmpty()){
            clientOrder.userComment = clientComment;
        }

        return clientOrder;
    }

    private OrderDetailsDM fromClientOrderDM(OrderDM clientOrder) {
        OrderDetailsDM order = new OrderDetailsDM();
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

    private void clearStoredOrder() {
        UserPreferencesManager.clearOrder(context);
        inActiveOrder = false;
        activeOrderId = -1;
        placedOrderDetailsDM = null;
        toggleButton(ButtonType.Place);

        cleanTaxiMarkerAndLocations();

        cleanDestinationLocationMarker();
        invalidateOptionsMenu();
    }

    private void cleanDestinationLocationMarker() {
        if(destinationLocationMarker != null){
            destinationLocationMarker.remove();
            destinationLocationMarker = null;
            destinationLocation = null;
            destinationAddress = "";
        }
    }

    private void cleanTaxiMarkerAndLocations() {
        if(taxiLocationMarker != null ){
            taxiLocationMarker.remove();
            taxiLocationMarker = null;
            taxiLocation = null;
        }
    }

    private void updateClientMarkers(String orderAddressTitle, boolean animate, boolean zoom) {
        clientLocationMarker = updateMarker(
                clientLocationMarker,
                new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude()),
                orderAddressTitle,
                R.drawable.person,
                animate,
                zoom
        );

        if(destinationLocation != null) {
                destinationLocationMarker = updateMarker(destinationLocationMarker,
                        new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude()),
                        destinationAddress,
                        R.drawable.destination,
                        false,
                        zoom
                );
        }
    }

    private void updateTaxiStands(Context context) {
        LocationDM location = new LocationDM();
        location.latitude = clientLocation.getLatitude();
        location.longitude = clientLocation.getLongitude();

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

    private boolean showToastError(RetrofitError error) {
        Response response = error.getResponse();
        if (response != null && response.getBody() != null) {
            String json =  new String(((TypedByteArray)error.getResponse().getBody()).getBytes());
            if(!json.isEmpty()){
                JsonObject jobj = new Gson().fromJson(json, JsonObject.class);
                String message = jobj.get("Message").getAsString();
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                // There was a message from the server
                return true;
            } else {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
        }

        return false;
    }

    private void addFavoriteLocation(LocationDM location) {
        showProgress(true);
        RestClientManager.addLocation(location, context, new Callback<LocationDM>() {
            @Override
            public void success(LocationDM locationDM, Response response) {
                int status = response.getStatus();
                showProgress(false);
                if (status == HttpStatus.SC_OK) {

                    favLocations.add(locationDM);
                    UserPreferencesManager.storeLocations(favLocations, context);

                    String storedAddressOK = String.format(getResources().getString(R.string.favorite_stored_successfully),
                            locationDM.title);
                    Toast.makeText(context, storedAddressOK, Toast.LENGTH_LONG).show();
                    return;
                }

                if (status == HttpStatus.SC_BAD_REQUEST) {
                    Toast.makeText(context, response.getBody().toString(), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            @Override
            public void failure(RetrofitError error) {
                showProgress(false);
                showToastError(error);
            }
        });
    }

    // USER INTERFACE
    private void initInputs() {
        // Hide addresses inputs
        startGroup = (RelativeLayout) this.findViewById(R.id.startGroup);
        startGroup.setVisibility(View.INVISIBLE);
        destinationGroup = (RelativeLayout)this.findViewById(R.id.destinationGroup);
        destinationGroup.setVisibility(View.INVISIBLE);
        commentGroup =  (RelativeLayout)this.findViewById(R.id.commentGroup);
        commentGroup.setVisibility(View.INVISIBLE);

        addToLocationsDrawable = getResources().getDrawable(android.R.drawable.ic_menu_myplaces);
        searchDrawable = getResources().getDrawable(android.R.drawable.ic_menu_search);

        // Addresses inputs
        startAddressEditText = (EditText) findViewById(R.id.startAddress);
        destinationAddressEditText = (EditText) findViewById(R.id.destinationAddress);

        startAddressButton = (ImageButton) findViewById(R.id.startAddress_btn);
        startAddressButton.setImageDrawable(searchDrawable);
        startFavoriteButton = (ImageButton) findViewById(R.id.startAddFavorite_btn);
        startFavoriteButton.setImageDrawable(addToLocationsDrawable);

        destinationAddressButton = (ImageButton) findViewById(R.id.destinationAddress_btn);
        destinationAddressButton.setImageDrawable(searchDrawable);
        destinationFavoriteButton = (ImageButton) findViewById(R.id.destinationAddFavorite_btn);
        destinationFavoriteButton.setImageDrawable(addToLocationsDrawable);

        // Comment input
        commentEditText =  (EditText) findViewById(R.id.commentEditText);

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

        startFavoriteButton.setEnabled(false);
        startFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentAddress.isEmpty() && clientLocation != null) {
                    LocationDM favoriteLocation = new LocationDM();
                    favoriteLocation.address = currentAddress;
                    favoriteLocation.latitude = clientLocation.getLatitude();
                    favoriteLocation.longitude = clientLocation.getLongitude();
                    enterAddressTitle(favoriteLocation);
                }
            }
        });

        destinationFavoriteButton.setEnabled(false);
        destinationFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!destinationAddress.isEmpty() && destinationLocation != null){
                    LocationDM favoriteLocation = new  LocationDM();
                    favoriteLocation.address = destinationAddress;
                    favoriteLocation.latitude = destinationLocation.getLatitude();
                    favoriteLocation.longitude = destinationLocation.getLongitude();
                    enterAddressTitle(favoriteLocation);
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
                showProgress(true);

                if(inActiveOrder){
                    // Order in progress, try to cancel it
                    RestClientManager.cancelOrder(activeOrderId, context, new Callback<OrderDM>() {
                        @Override
                        public void success(OrderDM cancelledOrderDM, Response response) {
                            showProgress(false);
                            int status = response.getStatus();
                            clearStoredOrder();
                            updateOrderInputs(cancelledOrderDM);
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
                            boolean hadMessage = showToastError(error);
                            //toggleButton(ButtonType.Cancel);
                            if(hadMessage){
                                cancelOrderButton.setEnabled(false);
                                //  clearStoredOrder();
                            }
                            if(error.getResponse() != null) {
                                int status = error.getResponse().getStatus();
                                if(status == HttpStatus.SC_NOT_FOUND) {
                                    clearStoredOrder();
                                }
                                if(status == HttpStatus.SC_CONFLICT){
                                    clearStoredOrder();
                                    Toast.makeText(context, getResources().getString(R.string.order_cancelled_already), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
                }
            }
        });

        placeOrderButton = (Button)findViewById(R.id.btn_place_order);
        if(clientUpdatedLocation != null && !inActiveOrder) {
            placeOrderButton.setEnabled(false);
        }
        placeOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeOrderButton.setEnabled(false);

                showProgress(true);

                // No order in progress, place new order
                OrderDM newClientOrder = prepareClientOrderDM();

                RestClientManager.addOrder(newClientOrder, context, new Callback<OrderDM>() {
                    @Override
                    public void success(OrderDM clientOrder, Response response) {
                        showProgress(false);
                        int status = response.getStatus();

                        if (status == HttpStatus.SC_OK) {
                            try {
                                placedOrderDetailsDM = fromClientOrderDM(clientOrder);
                                UserPreferencesManager.storeOrderId(clientOrder.orderId, context);
                                inActiveOrder = true;
                                activeOrderId = clientOrder.orderId;
                                initiateTracking(activeOrderId);
                                toggleButton(ButtonType.Cancel);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }

                            updateOrderInputs(placedOrderDetailsDM);
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

    private void cleanOrderEditTextInputs(){
        startAddressEditText.setText("");
        startAddressEditText.setEnabled(true);
        startAddressButton.setEnabled(true);

        destinationAddressEditText.setText("");
        destinationAddressEditText.setEnabled(true);
        destinationAddressButton.setEnabled(true);

        commentEditText.setText("");
    }

    private void updateOrderInputs(OrderDM orderDM){
        if(orderDM.status == Constants.OrderStatus.Unassigned.getValue() || orderDM.status == Constants.OrderStatus.Waiting.getValue()){
            startGroup.setVisibility(View.VISIBLE);
            startAddressEditText.setEnabled(false);
            startAddressButton.setVisibility(View.INVISIBLE);
            startAddressButton.setEnabled(false);
            startFavoriteButton.setVisibility(View.VISIBLE);
            startFavoriteButton.setEnabled(true);

            destinationGroup.setVisibility(View.VISIBLE);
            destinationAddressEditText.setVisibility(View.VISIBLE);
            destinationAddressButton.setVisibility(View.VISIBLE);
            destinationAddressEditText.setEnabled(true);
            destinationAddressButton.setEnabled(true);
            destinationFavoriteButton.setEnabled(true);
            if (orderDM.destinationAddress != null) {
                destinationAddressEditText.setText(orderDM.destinationAddress);
            }

            commentGroup.setVisibility(View.VISIBLE);
            if(orderDM.userComment != null && !orderDM.userComment.isEmpty()) {
                commentEditText.setText(orderDM.userComment);
            }

        } else if(orderDM.status == Constants.OrderStatus.InProgress.getValue()){
            startGroup.setVisibility(View.VISIBLE);
            startAddressEditText.setEnabled(false);
            startAddressButton.setVisibility(View.INVISIBLE);
            startAddressButton.setEnabled(false);
            startFavoriteButton.setVisibility(View.VISIBLE);
            startFavoriteButton.setEnabled(true);

            destinationGroup.setVisibility(View.VISIBLE);
            destinationAddressButton.setVisibility(View.INVISIBLE);
            destinationAddressButton.setEnabled(false);
            destinationAddressEditText.setEnabled(false);
            destinationFavoriteButton.setEnabled(true);

            if (orderDM.destinationAddress != null) {
                destinationAddressEditText.setVisibility(View.VISIBLE);
                destinationAddressEditText.setText(orderDM.destinationAddress);
            }

            commentGroup.setVisibility(View.INVISIBLE);

        } else if(orderDM.status == Constants.OrderStatus.Finished.getValue() || orderDM.status == Constants.OrderStatus.Cancelled.getValue()) {
            cleanOrderEditTextInputs();
            commentGroup.setVisibility(View.VISIBLE);
        }

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .show(locationsInputs)
                .commitAllowingStateLoss();
    }

    private void enterAddressTitle(final LocationDM favoriteLocation){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = getLayoutInflater();
        final View myView = inflater.inflate(R.layout.dialog_address_title, null);
        dialog.setTitle(R.string.dialog_title_favorite_address);
        dialog.setMessage(R.string.enter_favorite_address_msg);

        dialog.setView(myView);
        final EditText input = (EditText) myView.findViewById(R.id.enter_address_title);
        input.setText(favoriteLocation.address);
        dialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!input.getText().toString().isEmpty()) {
                            favoriteLocation.title = input.getText().toString();
                            addFavoriteLocation(favoriteLocation);
                        }
                    }
        });

        dialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        dialog.create().show();

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

            // Recent and accurate enough position is required
            if(clientUpdatedLocation != null && accuracy < Constants.LOCATION_ACCURACY_THRESHOLD) {
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

        if(lastReverseGeocodedLocation != null) {
            menu.findItem(R.id.add_to_locations).setVisible(true);
        } else {
            menu.findItem(R.id.add_to_locations).setVisible(false);
        }
        if(UserPreferencesManager.getTrackingState(context)){
            menu.findItem(R.id.tracking_location_cb).setChecked(true);
        } else {
            menu.findItem(R.id.tracking_location_cb).setChecked(false);
        }

        if(!inActiveOrder){
            //menu.findItem(R.id.select_start_location).setEnabled(true);
            menu.findItem(R.id.manage_profile_photo).setEnabled(true);
            menu.findItem(R.id.select_destination_location).setEnabled(true);
        } else {
            //menu.findItem(R.id.select_start_location).setEnabled(false);
            menu.findItem(R.id.manage_profile_photo).setEnabled(false);
            menu.findItem(R.id.select_destination_location).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id==R.id.manage_profile_photo){
            Intent gotoProfileIntent = new Intent(this, ProfileActivity.class);
            gotoProfileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(gotoProfileIntent);
        }

        if(id == R.id.add_to_locations){
            if(lastReverseGeocodedLocation != null){
                LocationDM favoriteLocation = new  LocationDM();
                favoriteLocation.address = currentAddress;
                favoriteLocation.latitude = lastReverseGeocodedLocation.getLatitude();
                favoriteLocation.longitude = lastReverseGeocodedLocation.getLongitude();
                enterAddressTitle(favoriteLocation);
            }
        }

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // LOCATIONS DIALOG
    private boolean ChooseLocationDialog(int id) {
//        if (id == R.id.enter_custom_locations) {
//            FragmentManager fm = this.getFragmentManager();
//            fm.beginTransaction()
//                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
//                    .show(locationsInputs)
//                    .commit();
//            return true;
//        }
        if (id == R.id.select_destination_location) {
            if(favLocations != null && !favLocations.isEmpty()) {
                Bundle bundle = new Bundle();
                Parcelable wrapped = Parcels.wrap(favLocations);
                bundle.putParcelable(Constants.USER_LOCATIONS, wrapped);
                locationDialog = new SelectLocationDialogFragment();

                FragmentManager fm = this.getFragmentManager();
                locationDialog.setArguments(bundle);

//                if (id == R.id.select_destination_location) {
                    destinationDialogTag = getResources().getString(R.string.select_destination_location);//locationDialog.getTag();
                    locationDialog.show(fm, destinationDialogTag);
                    return true;
//                }

//                startDialogTag = locationDialog.getTag();
//                locationDialog.show(fm, startDialogTag);
//                return true;
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
        dialog.dismiss();
    }

    @Override
    public void onLocationSelect(DialogFragment dialog, LocationDM locationDM) {
        // Location has been selected
        String tag = dialog.getTag();
        if(!inActiveOrder) {
            if (tag.equals(getResources().getString(R.string.select_destination_location))) {
                destinationAddressEditText.setText(locationDM.address);
                destinationAddress = locationDM.address;
                destinationLocation = new Location(Constants.LOCATION_FAVORITE);
                destinationLocation.setLatitude(locationDM.latitude);
                destinationLocation.setLongitude(locationDM.longitude);

            }
            //updateClientMarkers(locationDM.address, true, true);
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

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                lastZoom = cameraPosition.zoom;
                if (taxiLocationMarker != null) {
                    LatLng loc = new LatLng(taxiLocation.getLatitude(), taxiLocation.getLongitude());
                    animateMarker(taxiLocationMarker, loc, false);
                }

                if (clientLocationMarker != null) {
                    LatLng loc = new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude());
                    animateMarker(clientLocationMarker, loc, false);
                }

                if (destinationLocationMarker != null) {
                    LatLng loc = new LatLng(destinationLocation.getLatitude(), destinationLocation.getLongitude());
                    animateMarker(destinationLocationMarker, loc, false);
                }
            }
        });

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

    private Marker updateMarker(Marker marker, LatLng location, String title, int iconId, boolean animateEnabled, boolean zoom ){
        if (marker == null){

            MarkerOptions markerOpts = new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromResource(iconId))
                    .title(title);

            marker = mMap.addMarker(markerOpts);

        } else {
            marker.setTitle(title);
        }
        marker.showInfoWindow();
        if(animateEnabled) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 13));

            CameraPosition cameraPosition;
            if(zoom) {

                cameraPosition =new CameraPosition.Builder()
                        .target(location)      // Sets the center of the map to location user
                        .zoom(Constants.MAP_ANIMATION_ZOOM)                    // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
            } else {
                cameraPosition =new CameraPosition.Builder()
                        .target(location)      // Sets the center of the map to location user
                        .zoom(lastZoom)
                        .build();                   // Creates a CameraPosition from the builder
            }
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
                        updateMarker(destinationLocationMarker, latLng, resultAddress, R.drawable.destination, true, true);
                    }

                    // Client's current (start) position was reverse geocoded to get the
                    // position's address
                    if (addressTag == Constants.START_TAG) {

                        //Update inputs, startAddressEditText will hold the address to be sent
                        currentAddress = resultAddress;
                        startAddressEditText.setText(resultAddress);
                        startAddressButton.setImageDrawable(addToLocationsDrawable);
                        destinationGroup.setVisibility(View.VISIBLE);
                        destinationAddressEditText.setVisibility(View.VISIBLE);
                        destinationAddressButton.setVisibility(View.VISIBLE);
                        commentGroup.setVisibility(View.VISIBLE);
                        FragmentManager fm = getFragmentManager();
                        fm.beginTransaction()
                                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                .show(locationsInputs)
                                .commitAllowingStateLoss();

                        //Update marker with the latest result address
                        LatLng latLng = new LatLng(clientLocation.getLatitude(), clientLocation.getLongitude());
                        updateMarker(clientLocationMarker, latLng, resultAddress, R.drawable.person, true, true);

                        String addressLines[] = resultAddress.split("\\r?\\n");
                        //currentAddress = resultAddress;

                        lastReverseGeocodedLocation = clientLocation;
                        invalidateOptionsMenu();
                    }
                    // Show a toast message if an address was found.
                    Toast.makeText(context, R.string.address_found, Toast.LENGTH_LONG).show();

                }else if(geocodeType == Constants.GEOCODE){
                    // Search for an address was done and now its location and complete details are received
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
                        destinationAddress = resolvedAddress;
                        destinationLocation = new Location("destination");
                        destinationLocation.setLatitude(address.getLatitude());
                        destinationLocation.setLongitude(address.getLongitude());
                        destinationLocationMarker =
                                updateMarker(destinationLocationMarker, location, resolvedAddress, R.drawable.destination, true, true);
                        destinationAddressEditText.setText(destinationAddress);

                    }

                    if (addressTag == Constants.START_TAG) {

                        if(clientLocationMarker == null) {
                            clientLocationMarker =
                                    updateMarker(clientLocationMarker,location,resolvedAddress, R.drawable.person, true, true);
                        } else {
                            clientLocationMarker =
                                    updateMarker(clientLocationMarker,location,resolvedAddress, R.drawable.person, true, false);
                        }

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

    public static void showAlertDialog(int title, int message, final Context context) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        alertDialog.setTitle(title);

        alertDialog.setMessage(message);

        alertDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                       dialog.dismiss();
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

}
