package com.rest_api.app.controller;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.rest_api.app.entity.EarthquakeRequest;
import com.rest_api.app.service.EarthquakeService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class EarthquakeController {

    private final EarthquakeService earthquakeService;


    public EarthquakeController(EarthquakeService earthquakeService) {
        this.earthquakeService = earthquakeService;
    }

    @GetMapping("earthquakes")
    @PreAuthorize("hasRole('user.read')")
    @Scheduled(cron = "0 0 0 * * *")
    public void stageRawEarthquakeEvents() throws JsonMappingException, JsonProcessingException{

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(1);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        EarthquakeRequest request = EarthquakeRequest.builder()
            .starttime(startDate.format(formatter))
            .endtime(endDate.format(formatter))
            .build();
            
        earthquakeService.fetchEarthquakes(request);
    }
    
}