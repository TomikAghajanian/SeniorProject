package com.seniorproject.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.*;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class Coordinates {
    private GeoApiContext context;
    private String apiKey;
    private Gson gson;

    @Autowired
    public Coordinates(ReadApiKey readApiKey) {
        apiKey = readApiKey.getApiKey();
        gson = new GsonBuilder().setPrettyPrinting().create();
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    public GeocodingResult getCoordinates(String address) {
        GeocodingResult[] results = new GeocodingResult[0];
        try {
            results = GeocodingApi.geocode(this.context, address).await();
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (results.length > 0) {
            return results[0];
        }
        return null;
    }


}
