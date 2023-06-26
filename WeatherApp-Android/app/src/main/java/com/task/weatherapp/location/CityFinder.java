package com.task.weatherapp.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class CityFinder {

    public static void setLongitudeLatitude(Location location) {
        try {
            com.task.weatherapp.location.LocationCord.lat = String.valueOf(location.getLatitude());
            com.task.weatherapp.location.LocationCord.lon = String.valueOf(location.getLongitude());
            Log.d("location_lat", com.task.weatherapp.location.LocationCord.lat);
            Log.d("location_lon", com.task.weatherapp.location.LocationCord.lon);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static String getCityNameUsingNetwork(Context context, Location location) {
        String city = "";
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            city = addresses.get(0).getSubAdminArea() + ", " + addresses.get(0).getAdminArea();
            Log.d("city", city);
        } catch (Exception e) {
            Log.d("city", "Error to find the city.");
        }
        return city;
    }
}
