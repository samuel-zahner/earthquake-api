package com.rest_api.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;


@Service
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
                                 @Value("${external.api.population.dataset:wgpop}") String dataset) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pollTaskUrl = pollTaskUrl;
        this.year = year;
        this.dataset = dataset;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPopulationData(double lat, double lng, double radiusKm) throws Exception {
        String geojson = buildCircleGeoJson(lat, lng, radiusKm);

        // Build JSON body for POST
        Map<String, Object> requestBody = Map.of(
            "dataset", dataset, // or "wpgppop" depending on data needed
            "year", year,
            "geojson", geojson
        );

        // Call POST /v1/services/stats
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl,
            requestBody,
            Map.class
        );

        Map<?, ?> body = response.getBody();
        if (body == null || body.get("taskid") == null) {
            throw new IllegalStateException("WorldPop did not return a valid taskid");
        }

        String taskId = (String) body.get("taskid");

        // Poll for result until "finished"
        Map<?, ?> result = pollForResult(taskId);

        // Extract total population from the final data
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        if (data == null || !data.containsKey("agesexpyramid")) {
            throw new IllegalStateException("No age distribution data in WorldPop response");
        }

        return (List<Map<String, Object>>) data.get("agesexpyramid");
    }

    private Map<?, ?> pollForResult(String taskId) throws InterruptedException {
        String url = pollTaskUrl + taskId;
        for (int i = 0; i < 10; i++) {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if ("finished".equalsIgnoreCase((String) response.get("status"))) {
                return response;
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("Timed out waiting for WorldPop task: " + taskId);
    }


    private String buildCircleGeoJson(double lat, double lng, double radiusKm) {
        int points = 32; // number of points around circle
        double earthRadiusKm = 6371.0;
        List<List<Double>> coords = new ArrayList<>();

        for (int i = 0; i <= points; i++) {
            double angle = 2 * Math.PI * i / points;
            double dLat = (radiusKm / earthRadiusKm) * Math.sin(angle);
            double dLng = (radiusKm / earthRadiusKm) * Math.cos(angle) / Math.cos(Math.toRadians(lat));

            double newLat = lat + Math.toDegrees(dLat);
            double newLng = lng + Math.toDegrees(dLng);
            coords.add(Arrays.asList(newLng, newLat)); // GeoJSON uses [lng, lat]
        }

        Map<String, Object> polygon = Map.of(
                "type", "FeatureCollection",
                "features", List.of(
                        Map.of(
                                "type", "Feature",
                                "properties", Map.of(),
                                "geometry", Map.of(
                                        "type", "Polygon",
                                        "coordinates", List.of(coords)
                                )
                        )
                )
        );

        try {
            // Serialize once to JSON string
            String geojson = mapper.writeValueAsString(polygon);

            // Escape it again so it can be embedded as a JSON string literal
            return mapper.writeValueAsString(geojson);
        } catch (Exception e) {
            throw new RuntimeException("Error building GeoJSON", e);
        }
    }
}