package com.seniorproject.controller;

import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import com.seniorproject.services.Coordinates;
import com.seniorproject.services.Distance;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class CoordinatesController {
    private JSONObject finalResponse;


    @Autowired
    Coordinates coordinates;

    @Autowired
    Distance distance;

    @RequestMapping(value = "/get/coordinates", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> getAddressLatLong(@RequestBody String address) {
        JSONObject clientRequest = new JSONObject(address);
        finalResponse = new JSONObject();

        GeocodingResult results = coordinates.getCoordinates(clientRequest.getString("address"));

        if (results == null) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(this.finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        double lat = results.geometry.location.lat;
        double lng = results.geometry.location.lng;

        finalResponse.put("lat", lat);
        finalResponse.put("long", lng);

        return new ResponseEntity<>(this.finalResponse.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/get/distanceandtime", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<String> getAddressDirections(@RequestBody String request) {
        JSONObject addresses = new JSONObject(request);
        finalResponse = new JSONObject();

        DistanceMatrix distanceMatrix = distance.getDistanceAndTime(addresses.getString("origin"), addresses.getString("destination"));

        if (distanceMatrix == null) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(this.finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        finalResponse.put("distance", distanceMatrix.rows[0].elements[0].distance.humanReadable);
        finalResponse.put("time", distanceMatrix.rows[0].elements[0].duration.humanReadable);

        return new ResponseEntity<>(this.finalResponse.toString(), HttpStatus.OK);
    }
}
