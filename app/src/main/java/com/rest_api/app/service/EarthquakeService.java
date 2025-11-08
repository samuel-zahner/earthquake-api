package com.rest_api.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rest_api.app.repository.EarthquakeEventRepository;
import com.rest_api.app.repository.EarthquakeRequestRepository;
import com.rest_api.app.util.JsonResponseEnum;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.rest_api.app.entity.EarthquakeRequest;
import com.rest_api.app.entity.EarthquakeEvent;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class EarthquakeService {
    private EarthquakeRestService earthquakeRestService;

    private EarthquakeRequestRepository earthquakeRequestRepository;

    private EarthquakeEventRepository earthquakeEventRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public EarthquakeService(EarthquakeRestService earthquakeRestService,
                             EarthquakeRequestRepository earthquakeRequestRepository,
                             EarthquakeEventRepository earthquakeEventRepository) {
        this.earthquakeRestService = earthquakeRestService;
        this.earthquakeRequestRepository = earthquakeRequestRepository;
        this.earthquakeEventRepository = earthquakeEventRepository;
    }

    @Value("${earthquake.format}")
    private String responseFormat;
    

    public List<EarthquakeEvent> fetchEarthquakes(EarthquakeRequest request) {
        request.setRequestTime(new Timestamp(System.currentTimeMillis()));
        String response = earthquakeRestService.fetchEarthquakes(request, responseFormat);
        request.setResponseStatus(response);
        saveRequest(request);

        if (response == null || response.isEmpty()) {
            throw new IllegalArgumentException("Empty response from Earthquake API");
        }
        else if (!request.getResponseStatus().equals("200")){
            throw new IllegalArgumentException("Error response from Earthquake API: " + request.getResponseStatus());
       }
        JsonNode jsonRoot = mapper.readTree(response);

        List<EarthquakeEvent> earthquakeEvents = new ArrayList<>();

        if (jsonRoot.path("type").asString().equals(JsonResponseEnum.FEATURE_COLLECTION.getValue())) {
            try {
                earthquakeEvents.addAll(parseMultipleEarthquakeEvents(jsonRoot));
            } catch (Exception e) {
                log.error("Error parsing earthquake events", e);
            }
        } 
        else if (jsonRoot.path("type").asString().equals(JsonResponseEnum.FEATURE.getValue())) {
            EarthquakeEvent event = mapFeatureNodeToEarthquakeEvent(jsonRoot);
            earthquakeEvents.add(event);
        }
        else {
            throw new IllegalArgumentException("Unexpected GeoJSON type in response");
        }

        saveEarthquakeEvents(earthquakeEvents);
        return earthquakeEvents;
    }
    private void saveRequest(EarthquakeRequest request) {
        earthquakeRequestRepository.save(request);
    }

    private void saveEarthquakeEvents(List<EarthquakeEvent> events) {
        earthquakeEventRepository.saveAll(events);
    }

    /**
     * Parse a feed with multiple earthquakes
     */
    public List<EarthquakeEvent> parseMultipleEarthquakeEvents(JsonNode jsonRoot) throws Exception {
        List<EarthquakeEvent> earthquakeEvents = new ArrayList<>();
        for (JsonNode feature : jsonRoot.path("features")) {
            earthquakeEvents.add(mapFeatureNodeToEarthquakeEvent(feature));
        }
        return earthquakeEvents;
    }

    /**
     * Shared method to map a single Feature node to Earthquake
     */
    private EarthquakeEvent mapFeatureNodeToEarthquakeEvent(JsonNode feature) {
        JsonNode props = feature.path("properties");
        JsonNode coords = feature.path("geometry").path("coordinates");

        return EarthquakeEvent.builder()
                .earthquakeGlobalId(feature.path("id").asString())
                .magnitude(props.path("mag").isMissingNode() ? null : props.path("mag").asDouble())
                .magType(props.path("magType").asString(null))
                .place(props.path("place").asString(null))
                .time(convertToTimestamp(props.path("time").asLong()))
                .updated(convertToTimestamp(props.path("updated").asLong()))
                .tsunami(props.path("tsunami").asInt(0) == 1)
                .status(props.path("status").asString(null))
                .alert(props.path("alert").asString(null))
                .significance(props.path("sig").asInt())
                .network(props.path("net").asString(null))
                .code(props.path("code").asString(null))
                .types(props.path("types").asString(null))
                .longitude(coords.isArray() && coords.size() > 0 ? coords.get(0).asDouble() : null)
                .latitude(coords.isArray() && coords.size() > 1 ? coords.get(1).asDouble() : null)
                .depth(coords.isArray() && coords.size() > 2 ? coords.get(2).asDouble() : null)
                .url(props.path("url").asString(null))
                .detailUrl(props.path("detail").asString(null))
                .title(props.path("title").asString(null))
                .build();
    }

private java.sql.Timestamp convertToTimestamp(long epochMillis) {
    return epochMillis > 0 ? new java.sql.Timestamp(epochMillis) : null;
}
}
