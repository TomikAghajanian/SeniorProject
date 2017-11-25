package com.seniorproject.services;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;

import java.io.FileReader;

@Service
public class ReadApiKey {
    JSONParser parser = new JSONParser();

    public String getApiKey() {
        try {

            Object obj = null;
            try {
                obj = parser.parse(new FileReader(".idea/key.json"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject jsonObject = (JSONObject) obj;

            String key = (String) jsonObject.get("key");
            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
