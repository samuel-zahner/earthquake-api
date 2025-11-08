package com.rest_api.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.rest_api.app.entity.EarthquakeRequest;

@Service
public class EarthquakeRestService {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${earthquake.api.baseurl}")
    private String BASE_URL;

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

        return urlBuilder.toString();
    }

    public String fetchEarthquakes(EarthquakeRequest request, String format) {
        String uri = buildUriString(request, format);
        return restTemplate.getForObject(uri, String.class);
    }
}
