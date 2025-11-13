package com.earthquake_processor_api.app.service;

import com.earthquake_processor_api.app.entity.EarthquakeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EarthquakeRestServiceTest {

    private RestTemplate restTemplate;
    private EarthquakeRestService earthquakeRestService;

    private final String BASE_URL = "https://example.com/earthquakes";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        earthquakeRestService = new EarthquakeRestService(restTemplate, BASE_URL);
    }

    @Test
    void testBuildUriString_withAllParams() {
        EarthquakeRequest request = new EarthquakeRequest();
        request.setStarttime("2025-01-01");
        request.setEndtime("2025-01-02");
        request.setMinmagnitude("4.5");
        request.setLatitude("10.0");
        request.setLongitude("20.0");
        request.setMaxradiuskm("100.0");
        request.setOrderby("time");

        String uri = earthquakeRestService.buildUriString(request, "geojson");

        assertTrue(uri.startsWith(BASE_URL + "?format=geojson"));
        assertTrue(uri.contains("&starttime=2025-01-01"));
        assertTrue(uri.contains("&endtime=2025-01-02"));
        assertTrue(uri.contains("&minmagnitude=4.5"));
        assertTrue(uri.contains("&latitude=10.0"));
        assertTrue(uri.contains("&longitude=20.0"));
        assertTrue(uri.contains("&maxradiuskm=100.0"));
        assertTrue(uri.contains("&orderby=time"));
    }

    @Test
    void testBuildUriString_withPartialParams() {
        EarthquakeRequest request = new EarthquakeRequest();
        request.setMinmagnitude("5.0");

        String uri = earthquakeRestService.buildUriString(request, "geojson");

        assertEquals(BASE_URL + "?format=geojson&minmagnitude=5.0", uri);
    }

    @Test
    void testFetchEarthquakes_callsRestTemplate() {
        EarthquakeRequest request = new EarthquakeRequest();
        String uri = BASE_URL + "?format=geojson";

        ResponseEntity<String> mockResponse = ResponseEntity.ok("{\"type\":\"FeatureCollection\"}");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(String.class)))
                .thenReturn(mockResponse);

        ResponseEntity<String> response = earthquakeRestService.fetchEarthquakes(request, "geojson");

        System.out.println("Response: " + response.getStatusCode().value());
        assertNotNull(response);
        assertTrue(response.getStatusCode().value() == 200);
        assertEquals("{\"type\":\"FeatureCollection\"}", response.getBody());

        // Verify correct URI was used
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(captor.capture(), eq(HttpMethod.GET), isNull(), eq(String.class));
        assertTrue(captor.getValue().startsWith(BASE_URL));
    }
}

