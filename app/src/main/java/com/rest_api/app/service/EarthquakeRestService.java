package com.rest_api.app.service;

import com.rest_api.app.entity.EarthquakeRequest;

public class EarthquakeRestService {
    
    private static final String BASE_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query";

    public String buildUriString(EarthquakeRequest request, String format) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        urlBuilder.append("?format=").append(format);

        if (request.getStarttime() != null) {
            urlBuilder.append("&starttime=").append(request.getStarttime());
        }
        if (request.getEndtime() != null) {
            urlBuilder.append("&endtime=").append(request.getEndtime());
        }
        if (request.getMinmagnitude() != null) {
            urlBuilder.append("&minmagnitude=").append(request.getMinmagnitude());
        }
        if (request.getLatitude() != null) {
            urlBuilder.append("&latitude=").append(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            urlBuilder.append("&longitude=").append(request.getLongitude());
        }
        if (request.getMaxradiuskm() != null) {
            urlBuilder.append("&maxradiuskm=").append(request.getMaxradiuskm());
        }
        if (request.getOrderby() != null) {
            urlBuilder.append("&orderby=").append(request.getOrderby());
        }

        String finalUrl = urlBuilder.toString();
        // Code to make HTTP GET request to finalUrl and return the response as a String
    }
}
