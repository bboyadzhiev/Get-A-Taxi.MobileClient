package com.getataxi.client.utils;

/**
 * Created by bvb on 21.4.2015 г..
 */
public final class Constants {


    public static final String PACKAGE_NAME = Constants.class.getPackage().getName();




    // GEOCODE SERVICE
    public static final String GEOCODE_TAG = PACKAGE_NAME + ".GEOCODE_TAG"; // Tag of the geocode
    public static final int START_TAG = 0;
    public static final int DESTINATION_TAG = 1;

    public static final String GEOCODE_TYPE =  "GEOCODE_TYPE"; // Type of geocode
    public static final int GEOCODE = 0;    // Get the location of an address
    public static final int REVERSE_GEOCODE = 1; // Get the address of a location


    // GEOCODE RESULT
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String GEOCODE_RECEIVER = PACKAGE_NAME + ".GEOCODE_RECEIVER";

    public static final String ADDRESS_DATA_EXTRA = PACKAGE_NAME + ".ADDRESS_DATA_EXTRA";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    //LOCATION SERVICE
    public static final String LOCATION_UPDATED = PACKAGE_NAME + "LOCATION_UPDATED";
    public static final String LOCATION = PACKAGE_NAME + ".LOCATION";

    public static final String LOCATION_REPORT_ENABLED = PACKAGE_NAME + ".LOCATION_REPORT_ENABLED";
    public static final String LOCATION_REPORT_TITLE = PACKAGE_NAME + ".LOCATION_REPORT_TITLE";

    public static final int LOCATION_UPDATE_INTERVAL = 10000; // milliseconds
    public static final int LOCATION_UPDATE_DISTANCE = 1; // meters

    public static final int LOCATION_TIMEOUT = 1000 * 60 * 5; // five minutes;


    public static final String USER_LOCATIONS =  PACKAGE_NAME + ".USER_LOCATIONS";

    // DEBUGGING STRINGS

}