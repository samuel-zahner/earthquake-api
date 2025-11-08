package com.rest_api.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.rest_api.app.entity.EarthquakeRequest;

@Service
public class EarthquakeRestService {

    private final RestTemplate restTemplate;
    private String base_url;

    public EarthquakeRestService(RestTemplate restTemplate,
                                 @Value("${earthquake.api.baseurl:}") String base_url) {
        this.restTemplate = restTemplate;
        this.base_url = base_url;
    }

    public String buildUriString(EarthquakeRequest request, String format) {
        StringBuilder urlBuilder = new StringBuilder(base_url);
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
