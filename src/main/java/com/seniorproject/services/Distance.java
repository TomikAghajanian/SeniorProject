package com.seniorproject.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import org.springframework.stereotype.Service;

@Service
public class Distance {
    private GeoApiContext context;
    private final String API_KEY = "AIzaSyBenRTIwXQY_cGIDJtDGR15vHanewFRfIg";
    private Gson gson;

    public Distance() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        this.context = new GeoApiContext.Builder()
                .apiKey(API_KEY)
                .build();
    }

    public DistanceMatrix getDistanceAndTime(String origin, String destination) {
        try {
            DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(this.context).origins(origin).destinations(destination).await();
            if (distanceMatrix.rows.length == 0 || distanceMatrix.rows[0].elements.length == 0) {
                throw new RuntimeException("No distance and duration found.");
            }
            return distanceMatrix;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
