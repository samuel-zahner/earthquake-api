package com.earthquake_processor_api.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PopulationRestServiceTest {

    private RestTemplate restTemplate;
    private PopulationRestService populationRestService;

    private final String BASE_URL = "https://example.com/population";
    private final String POLL_URL = "https://example.com/population/poll/";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        populationRestService = new PopulationRestService(restTemplate, BASE_URL, POLL_URL, 2020, "wpgpas");
    }

    @Test
    void testBuildCircleGeoJson_notEmpty() throws Exception {
        // reflection used to call private method
        var method = PopulationRestService.class.getDeclaredMethod("buildCircleGeoJson", double.class, double.class, double.class);
        method.setAccessible(true);

        String geoJson = (String) method.invoke(populationRestService, 10.0, 20.0, 5.0);
        assertNotNull(geoJson);
        assertTrue(geoJson.contains("\"type\":\"FeatureCollection\""));
        assertTrue(geoJson.contains("\"type\":\"Polygon\""));
    }

    @Test
    void testPollForResult_returnsEmptyOnError() throws Exception {
        // simulate task returning error
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "finished");
        errorResponse.put("error", true);
        errorResponse.put("error_message", "Simulated error");

        when(restTemplate.getForObject(POLL_URL + "123", Map.class)).thenReturn(errorResponse);

        var method = PopulationRestService.class.getDeclaredMethod("pollForResult", String.class);
        method.setAccessible(true);
        Map<?, ?> result = (Map<?, ?>) method.invoke(populationRestService, "123");

        assertTrue(result.isEmpty());
        verify(restTemplate).getForObject(POLL_URL + "123", Map.class);
    }
}

