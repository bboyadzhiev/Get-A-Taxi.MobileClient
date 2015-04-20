package com.getataxi.client.models;

/**
 * Created by bvb on 5.4.2015 г..
 */
public class Location {
    private double longitude;
    private double latitude;

    public Location(double lat, double lon){
        this.setLatitude(lat);
        this.setLongitude(lon);
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
