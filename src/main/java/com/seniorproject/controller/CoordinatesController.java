package com.seniorproject.controller;

import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.GeocodingResult;
import com.seniorproject.services.IDatabase;
import com.seniorproject.services.ILocationService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController

public class CoordinatesController {
    @Autowired
    ILocationService locationService;

    static int id = 1;
    @Autowired
    IDatabase database;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @RequestMapping(value="/login", method = RequestMethod.GET)
    public ResponseEntity<String> login(){
        return new ResponseEntity<>(new String("hello"), HttpStatus.OK);
    }

    @RequestMapping(value = "/coordinates", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getAddressLatLong(@RequestParam(value = "address") String address) {
        JSONObject finalResponse = new JSONObject();
        logger.debug("hi from logger");
        GeocodingResult results;
        try {
            results = locationService.getCoordinates(address);
        } catch (Exception e) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (results == null || results.geometry == null || results.geometry.location == null) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        finalResponse.put("lat", results.geometry.location.lat);
        finalResponse.put("long", results.geometry.location.lng);

        return new ResponseEntity<>(finalResponse.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/distanceandduration", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> getAddressDirections(@RequestParam Map<String, String> requestParams) {
        JSONObject finalResponse = new JSONObject();
        String origin;
        String destination;
        try {
            origin = requestParams.get("origin");
            destination = requestParams.get("destination");

            database.inputClientSession(id++, origin+destination);
        } catch (Exception e) {
            finalResponse.put("error", "Request parameters \"origin\" and \"destination\" not found.");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.BAD_REQUEST);
        }

        DistanceMatrix distanceMatrix;
        try {
            distanceMatrix = locationService.getDistanceAndTime(origin, destination);
        } catch (Exception e) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (distanceMatrix == null) {
            finalResponse.put("error", "Internal server error");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (distanceMatrix.rows[0] == null || distanceMatrix.rows[0].elements[0] == null) {
            finalResponse.put("error", "Unexpected error");
            return new ResponseEntity<>(finalResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        finalResponse.put("distance", distanceMatrix.rows[0].elements[0].distance.humanReadable);
        finalResponse.put("time", distanceMatrix.rows[0].elements[0].duration.humanReadable);

        return new ResponseEntity<>(finalResponse.toString(), HttpStatus.OK);
    }
}
