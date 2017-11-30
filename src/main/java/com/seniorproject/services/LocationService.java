package com.seniorproject.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LocationService implements ILocationService {
    private GeoApiContext context;
    private Gson gson;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    public LocationService(IDatabase database) {
        gson = new GsonBuilder().setPrettyPrinting().create();
        this.context = new GeoApiContext.Builder()
                .apiKey(database.getApiKey())
                .build();
    }

    public GeocodingResult getCoordinates(String address) {
        GeocodingResult[] results = new GeocodingResult[0];
        try {
            results = GeocodingApi.geocode(this.context, address).await();
        } catch (ApiException e) {
            logger.error("error", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("error", e);
        } catch (IOException e) {
            logger.error("error", e);
            return null;
        }
        if (results.length > 0) {
            return results[0];
        }
        return null;
    }

    public DistanceMatrix getDistanceAndTime(String origin, String destination) {
        try {
            DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(this.context).origins(origin).destinations(destination).await();
            if (distanceMatrix.rows.length == 0 || distanceMatrix.rows[0].elements.length == 0) {
                throw new RuntimeException("No distance and duration found.");
            }
            return distanceMatrix;
        } catch (Exception e) {
            logger.error("error", e);
            return null;
        }
    }

    public GeocodingResult getAddress(String lat, String lng) {
        GeocodingResult[] results = new GeocodingResult[0];
        double latD = Double.valueOf(lat);
        double lngD = Double.valueOf(lng);
        LatLng latLng = new LatLng(latD, lngD);

        try {
            results = GeocodingApi.reverseGeocode(this.context, latLng).await();
        } catch (ApiException e) {
            logger.error("error", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("error", e);
        } catch (IOException e) {
            logger.error("error", e);
            return null;
        }
        if (results.length > 0) {
            return results[0];
        }

        logger.error("error", "no results where found");
        return null;
    }
}
