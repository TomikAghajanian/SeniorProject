package com.seniorproject.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Distance {
    private GeoApiContext context;
    private String apiKey;
    private Gson gson;

    @Autowired
    public Distance(ReadApiKey readApiKey) {
        apiKey = readApiKey.getApiKey();
        gson = new GsonBuilder().setPrettyPrinting().create();
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
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
