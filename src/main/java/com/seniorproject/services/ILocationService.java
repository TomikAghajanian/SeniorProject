package com.seniorproject.services;

import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;

public interface ILocationService {
    GeocodingResult getCoordinates(String address);
    DistanceMatrix getDistanceAndTime(String origin, String destination);
    GeocodingResult getAddress(String lat, String lng);
}
