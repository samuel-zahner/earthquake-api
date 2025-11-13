package com.earthquake_processor_api.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.earthquake_processor_api.app.repository.EarthquakeEventRepository;
import com.earthquake_processor_api.app.repository.EarthquakeRequestRepository;
import com.earthquake_processor_api.app.util.JsonResponseEnum;

import lombok.extern.slf4j.Slf4j;

import com.earthquake_processor_api.app.entity.EarthquakeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.earthquake_processor_api.app.entity.EarthquakeEvent;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    @Value("${external.api.earthquake.format:geojson}")
    private String responseFormat;
    
    /**
     * Fetch earthquakes from external API
     * @param request earthquake request
     * @return list of earthquake events
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    public List<EarthquakeEvent> fetchEarthquakes(EarthquakeRequest request) throws JsonMappingException, JsonProcessingException {
        request.setRequestTime(new Timestamp(System.currentTimeMillis()));
        ResponseEntity<String> response = earthquakeRestService.fetchEarthquakes(request, responseFormat);

        if (response == null) {
            throw new IllegalArgumentException("Empty response from US Earthquake API");
        }

        request.setResponseStatus(response.getStatusCode().toString());
        saveRequest(request);

        if (!request.getResponseStatus().equals(HttpStatus.OK.toString())) {
            throw new IllegalArgumentException("Error response from Earthquake API: " + request.getResponseStatus());
       }
        JsonNode jsonRoot = mapper.readTree(response.getBody());

        List<EarthquakeEvent> earthquakeEvents = new ArrayList<>();

        if (jsonRoot.path("type").asText().equals(JsonResponseEnum.FEATURE_COLLECTION.getValue())) {
            try {
                earthquakeEvents.addAll(parseMultipleEarthquakeEvents(jsonRoot, request));
            } catch (Exception e) {
                log.error("Error parsing earthquake events", e);
            }
        } 
        else if (jsonRoot.path("type").asText().equals(JsonResponseEnum.FEATURE.getValue())) {
            EarthquakeEvent event = mapFeatureNodeToEarthquakeEvent(jsonRoot, request);
            earthquakeEvents.add(event);
        }
        else {
            throw new IllegalArgumentException("Unexpected GeoJSON type in response");
        }

        saveEarthquakeEvents(earthquakeEvents);
        return earthquakeEvents;
    }

    /**
     * Save earthquake request
     * @param request earthquake request
     */
    private void saveRequest(EarthquakeRequest request) {
        earthquakeRequestRepository.save(request);
    }

    /**
     * Save earthquake events
     * @param events list of earthquake events
     */
    private void saveEarthquakeEvents(List<EarthquakeEvent> events) {
        earthquakeEventRepository.saveAll(events);
    }

    /**
     * Parse multiple earthquake events from GeoJSON FeatureCollection
     * @param jsonRoot JSON root node
     * @param request earthquake request
     * @return list of earthquake events
     * @throws Exception on parsing errors
     */
    public List<EarthquakeEvent> parseMultipleEarthquakeEvents(JsonNode jsonRoot, EarthquakeRequest request) throws Exception {
        List<EarthquakeEvent> earthquakeEvents = new ArrayList<>();
        for (JsonNode feature : jsonRoot.path("features")) {
            earthquakeEvents.add(mapFeatureNodeToEarthquakeEvent(feature, request));
        }
        return earthquakeEvents;
    }

    /**
     * Map GeoJSON Feature node to EarthquakeEvent entity
     * @param feature JSON feature node
     * @param request earthquake request
     * @return EarthquakeEvent entity
     */
    private EarthquakeEvent mapFeatureNodeToEarthquakeEvent(JsonNode feature, EarthquakeRequest request) {
        JsonNode props = feature.path("properties");
        JsonNode coords = feature.path("geometry").path("coordinates");

        return EarthquakeEvent.builder()
                .earthquakeGlobalId(feature.path("id").asText())
                .magnitude(props.path("mag").isMissingNode() ? null : props.path("mag").asDouble())
                .magType(props.path("magType").asText(null))
                .place(props.path("place").asText(null))
                .time(convertToTimestamp(props.path("time").asLong()))
                .updated(convertToTimestamp(props.path("updated").asLong()))
                .tsunami(props.path("tsunami").asInt(0) == 1)
                .status(props.path("status").asText(null))
                .alert(props.path("alert").asText(null))
                .significance(props.path("sig").asInt())
                .network(props.path("net").asText(null))
                .code(props.path("code").asText(null))
                .types(props.path("types").asText(null))
                .longitude(coords.isArray() && coords.size() > 0 ? coords.get(0).asDouble() : null)
                .latitude(coords.isArray() && coords.size() > 1 ? coords.get(1).asDouble() : null)
                .depth(coords.isArray() && coords.size() > 2 ? coords.get(2).asDouble() : null)
                .url(props.path("url").asText(null))
                .detailUrl(props.path("detail").asText(null))
                .title(props.path("title").asText(null))
                .request(request)
                .build();
    }

    /**
     * Convert epoch milliseconds to SQL Timestamp
     * @param epochMillis epoch milliseconds
     * @return SQL Timestamp
     */
    private java.sql.Timestamp convertToTimestamp(long epochMillis) {
        return epochMillis > 0 ? new java.sql.Timestamp(epochMillis) : null;
    }

    /**
     * Extracts the nearest city name from a USGS-style "place" string.
     * Example:
     *  "16 km S of Volcano, Hawaii" -> "Volcano, Hawaii"
     *  "54 km NW of San Antonio, Chile" -> "San Antonio, Chile"
     *  "Near the coast of Central Chile" -> "Central Chile"
     * @param place USGS place string
     * @return nearest city name
     */
    public String extractNearestCity(String place) {
        if (place == null || place.isBlank()) {
            return place;
        }

        // USGS format: "<distance> <direction> of <city, region>"
        String[] parts = place.split("\\sof\\s", 2);

        if (parts.length == 2) {
            return parts[1].trim(); 
        }

        // fallback for cases like "Near the coast of Central Chile"
        if (place.toLowerCase().startsWith("near")) {
            String[] tokens = place.split("\\s+");
            if (tokens.length >= 2) {
                return String.join(" ", Arrays.copyOfRange(tokens, tokens.length - 2, tokens.length));
            }
        }

        // If nothing else fits, return original
        return place.trim();
    }

    /**
     * Extract distance to nearest city in kilometers from USGS-style "place" string.
     * Examples:
     *  "16 km S of Volcano, Hawaii" -> 16.0
     *  "54 km NW of San Antonio, Chile" -> 54.0
     *  "10 mi W of Los Angeles, California" -> 16.0934
     * @param place USGS place string
     * @return distance to nearest city in kilometers
     */
    public Double extractDistanceToNearestCityKm(String place) {
        if (place == null || place.isBlank()) {
            return null;
        }

        // Match number followed by unit (km or mi)
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(km|mi)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(place);

        if (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            // Convert miles to kilometers if needed
            if (unit.equals("mi")) {
                value *= 1.60934;
            }
            return value;
        }

        return null; // no distance found
    }

    /**
     * Determine if an earthquake is significant based on multiple criteria
     * @param magnitude earthquake magnitude
     * @param tsunami tsunami flag
     * @param alertLevel USGS alert level
     * @param population100km population within 100 km
     * @param distanceToNearestCityKm distance to nearest city in kilometers
     * @return true if significant, false otherwise
     */
    public boolean isSignificantEarthquake(Double magnitude,
                                            Boolean tsunami,
                                            String alertLevel,
                                            Double population100km,
                                            Double distanceToNearestCityKm){

        if (magnitude == null) return false;

        //Major earthquakes (6.0+) are always significant
        if (magnitude >= 6.0) {
            return true;
        }

        //Moderate quakes (5.0–6.0) near cities can be significant
        if (magnitude >= 5.0 && distanceToNearestCityKm != null && distanceToNearestCityKm <= 30) {
            return true;
        }

        //Smaller quakes (<5.0) — only if extremely close and population-dense
        if (magnitude >= 4.0 && distanceToNearestCityKm != null && distanceToNearestCityKm <= 10
                && population100km != null && population100km >= 1_000_000) {
            return true;
        }

        //Tsunami flag is always significant
        if (Boolean.TRUE.equals(tsunami)) {
            return true;
        }

        //USGS alert levels
        if (alertLevel != null) {
            List<String> significantAlerts = Arrays.asList("yellow", "orange", "red");
            if (significantAlerts.contains(alertLevel.toLowerCase())) {
                return true;
            }
        }

        //Extremely dense population exposure
        if (population100km != null && population100km >= 5_000_000) {
            return true;
        }

        return false;
    }

}
