package com.rest_api.app.service;

import org.springframework.beans.factory.annotation.Value;
import com.rest_api.app.repository.EarthquakeEventRepository;
import com.rest_api.app.repository.EarthquakeRequestRepository;
import com.rest_api.app.entity.EarthquakeRequest;
import com.rest_api.app.entity.EarthquakeEvent;
import java.util.List;

public class EarthquakeService {

    private EarthquakeRestService earthquakeRestService;

    private EarthquakeRequestRepository earthquakeRequestRepository;

    private EarthquakeEventRepository earthquakeEventRepository;

    @Value("${earthquake.format}")
    private String responseFormat;
    

    public List<EarthquakeEvent> fetchEarthquakes(EarthquakeRequest request) {
        saveRequest(request);
        String response = earthquakeRestService.fetchEarthquakes(request, responseFormat);
        List<EarthquakeEvent> earthquakeEvents = parseResponse(response);
        saveEarthquakeEvents(earthquakeEvents);
        return earthquakeEvents;
    }

    private void saveRequest(EarthquakeRequest request) {
        earthquakeRequestRepository.save(request);
    }

    private List<EarthquakeEvent> parseResponse(String response) {
        // Implementation to parse the response and convert it into a list of EarthquakeEvent entities
    }

    private void saveEarthquakeEvents(List<EarthquakeEvent> events) {
        earthquakeEventRepository.saveAll(events);
    }
}
