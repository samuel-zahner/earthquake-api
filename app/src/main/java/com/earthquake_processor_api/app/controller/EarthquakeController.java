package com.earthquake_processor_api.app.controller;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.earthquake_processor_api.app.entity.EarthquakeRequest;
import com.earthquake_processor_api.app.service.EarthquakeService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@Tag(name = "Earthquake Controller", description = "Controller to fetch and stage earthquake events")
public class EarthquakeController {

    private final EarthquakeService earthquakeService;


    public EarthquakeController(EarthquakeService earthquakeService) {
        this.earthquakeService = earthquakeService;
    }

    @GetMapping("earthquakes")
    @PreAuthorize("hasRole('user.read')")
    @Operation(summary = "Fetch Earthquake Events", description = "Fetches earthquake events from the external API for the past day, and stages them into the database for processing.")
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