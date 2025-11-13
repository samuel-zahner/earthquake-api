package com.earthquake_processor_api.app.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PopulationRestService {

    private final RestTemplate restTemplate;
    private String baseUrl;
    private String pollTaskUrl;
    private int year;
    private String dataset;
    private final ObjectMapper mapper = new ObjectMapper();

    public PopulationRestService(RestTemplate restTemplate,
                                 @Value("${external.api.population.base-url:}") String baseUrl,
                                 @Value("${external.api.population.poll-task-url:}") String pollTaskUrl,
                                 @Value("${external.api.population.year:2020}") int year,
                                 @Value("${external.api.population.dataset:wpgpas}") String dataset) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pollTaskUrl = pollTaskUrl;
        this.year = year;
        this.dataset = dataset;
    }

    /**
     * Fetch population data from WorldPop API
     * @param lat latitude
     * @param lng longitude
     * @param radiusKm radius in kilometers
     * @return list of population data maps
     * @throws Exception on errors
     */
    public List<Map<String, Object>> fetchPopulationData(double lat, double lng, double radiusKm) throws Exception {
        String geojson = buildCircleGeoJson(lat, lng, radiusKm);
        WebClient client = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        String encoded = URLEncoder.encode(geojson, StandardCharsets.UTF_8);
        URI uri = URI.create(baseUrl + "?dataset=" + dataset + "&year=" + year + "&geojson=" + encoded);
        
        Map<?,?> responseBody = client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        if (responseBody == null || responseBody.get("taskid") == null) {
            log.error("WorldPop did not return a valid taskid");
            return Collections.emptyList();
        }

        String taskId = (String) responseBody.get("taskid");

        // Poll for result until "finished"
        Map<?, ?> result = pollForResult(taskId);

        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }
        // Extract total population from the final data
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        if (data == null || !data.containsKey("agesexpyramid")) {
            log.error("No age distribution data in WorldPop response");
            return Collections.emptyList();
        }

        return (List<Map<String, Object>>) data.get("agesexpyramid");
    }

    /**
     * Poll for WorldPop task result
     * @param taskId task ID
     * @return result map
     * @throws InterruptedException on interruption
     */
    private Map<?, ?> pollForResult(String taskId) throws InterruptedException {
        String url = pollTaskUrl + taskId;
        for (int i = 0; i < 10; i++) {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if ("finished".equalsIgnoreCase((String) response.get("status"))) {
                if ((boolean)response.get("error")) {
                    log.error("WorldPop task error: " + response.get("error_message"));
                    return Collections.emptyMap();
                }
                else{
                    return response;
                }
            }
            Thread.sleep(1000);
        }
        log.error("Timed out (10 seconds) waiting for WorldPop task: " + taskId);
        return Collections.emptyMap();
    }


    /**
     * Build GeoJSON circle around lat/lng with given radius
     * @param lat latitude
     * @param lng longitude
     * @param radiusKm radius in kilometers
     * @return GeoJSON string
     */
    private String buildCircleGeoJson(double lat, double lng, double radiusKm) {
        int points = 32;
        double earthRadiusKm = 6371.0;
        List<List<Double>> coords = new ArrayList<>();

        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double dLat = (radiusKm / earthRadiusKm) * Math.sin(angle);
            double dLng = (radiusKm / earthRadiusKm) * Math.cos(angle) / Math.cos(Math.toRadians(lat));
            double newLat = lat + Math.toDegrees(dLat);
            double newLng = lng + Math.toDegrees(dLng);
            coords.add(Arrays.asList(newLng, newLat));
        }

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Polygon");
        geometry.put("coordinates", List.of(coords));

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("properties", Map.of());
        feature.put("geometry", geometry);

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection"); // must come first
        featureCollection.put("features", List.of(feature));

        try {
            // Single serialization to proper escaped JSON string
            return new ObjectMapper().writeValueAsString(featureCollection);
        } catch (Exception e) {
            log.error("Error building GeoJSON", e);
            return "";
        }
    }
}