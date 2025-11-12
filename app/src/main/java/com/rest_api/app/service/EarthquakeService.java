package com.rest_api.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    @Value("${external.api.earthquake.format}")
    private String responseFormat;
    

    public List<EarthquakeEvent> fetchEarthquakes(EarthquakeRequest request) {
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

    /**
    * Extracts the nearest city and region from a USGS-style "place" string.
    * Examples:
    *  "16 km S of Volcano, Hawaii" -> "Volcano, Hawaii"
    *  "54 km NW of San Antonio, Chile" -> "San Antonio, Chile"
    *  "Near the coast of Central Chile" -> "Central Chile"
    */
    public String extractNearestCity(String place) {
        if (place == null || place.isBlank()) {
            return place;
        }

        // USGS format: "<distance> <direction> of <city, region>"
        // Split on " of " (note the spaces)
        String[] parts = place.split("\\sof\\s", 2);

        if (parts.length == 2) {
            return parts[1].trim(); // part after "of" is the nearest city
        }

        // fallback for cases like "Near the coast of Central Chile"
        if (place.toLowerCase().startsWith("near")) {
            // try to take last two words (rough approximation)
            String[] tokens = place.split("\\s+");
            if (tokens.length >= 2) {
                return String.join(" ", Arrays.copyOfRange(tokens, tokens.length - 2, tokens.length));
            }
        }

        // If nothing else fits, return original
        return place.trim();
    }

    /**
    * Extracts the distance in kilometers from a USGS-style "place" string.
    * Example:
    *  "16 km S of Volcano, Hawaii" -> 16.0
    *  "54 km NW of San Antonio, Chile" -> 54.0
    *  "Near the coast of Central Chile" -> null
    *  "10 mi NE of Ridgecrest, CA" -> 16.1 (converted from miles to km)
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

    public boolean isSignificantEarthquake(Double magnitude,
                                            Boolean tsunami,
                                            String alertLevel,
                                            Double population100km,
                                            Double distanceToNearestCityKm){

        if (magnitude == null) return false;

        // 1️⃣ Major earthquakes (6.0+) are always significant
        if (magnitude >= 6.0) {
            return true;
        }

        // 2️⃣ Moderate quakes (5.0–6.0) near cities can be significant
        if (magnitude >= 5.0 && distanceToNearestCityKm != null && distanceToNearestCityKm <= 30) {
            return true;
        }

        // 3️⃣ Smaller quakes (<5.0) — only if extremely close and population-dense
        if (magnitude >= 4.0 && distanceToNearestCityKm != null && distanceToNearestCityKm <= 10
                && population100km != null && population100km >= 1_000_000) {
            return true;
        }

        // 4️⃣ Tsunami flag is always significant
        if (Boolean.TRUE.equals(tsunami)) {
            return true;
        }

        // 5️⃣ USGS alert levels
        if (alertLevel != null) {
            List<String> significantAlerts = Arrays.asList("yellow", "orange", "red");
            if (significantAlerts.contains(alertLevel.toLowerCase())) {
                return true;
            }
        }

        // 6️⃣ Extremely dense population exposure
        if (population100km != null && population100km >= 5_000_000) {
            return true;
        }

        return false;
    }

}
