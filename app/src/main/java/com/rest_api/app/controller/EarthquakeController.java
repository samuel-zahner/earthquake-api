package com.rest_api.app.controller;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.rest_api.app.entity.EarthquakeRequest;
import com.rest_api.app.service.EarthquakeService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rest_api.app.entity.EarthquakeEvent;


@RestController
public class EarthquakeController {

    private final EarthquakeService earthquakeService;


    public EarthquakeController(EarthquakeService earthquakeService) {
        this.earthquakeService = earthquakeService;
    }

    @GetMapping("earthquakes")
    public List<EarthquakeEvent> getMethodName(@RequestParam(value="starttime", required=false) String starttime,
                                @RequestParam(value="endtime", required=false) String endtime,
                                @RequestParam(value="minmagnitude", required=false) String minmagnitude,
                                @RequestParam(value="latitude", required=false) String latitude,
                                @RequestParam(value="longitude", required=false) String longitude,
                                @RequestParam(value="maxradiuskm", required=false) String maxradiuskm,
                                @RequestParam(value="orderby", required=false) String orderby){

        EarthquakeRequest request = EarthquakeRequest.builder()
            .starttime(starttime)
            .endtime(endtime)
            .minmagnitude(minmagnitude)
            .latitude(latitude)
            .longitude(longitude)
            .maxradiuskm(maxradiuskm)
            .orderby(orderby)
            .build();
            
            return earthquakeService.fetchEarthquakes(request);
    }
    
}