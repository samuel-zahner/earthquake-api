package com.earthquake_processor_api.app.service;

import com.earthquake_processor_api.app.entity.PopulationDemographics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PopulationServiceTest {

    private PopulationRestService populationRestService;
    private PopulationService populationService;

    @BeforeEach
    void setUp() {
        populationRestService = mock(PopulationRestService.class);
        populationService = new PopulationService(populationRestService);
    }

    @Test
    void testFetchPopulation_EmptyData() throws Exception {
        when(populationRestService.fetchPopulationData(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        PopulationDemographics result = populationService.fetchPopulation(0, 0, 10);

        assertNotNull(result);
        assertEquals(0.0, result.getTotalPopulation());
        assertEquals(0.0, result.getAvgAge());
        assertEquals(0.0, result.getPercentMale());
        assertEquals(0.0, result.getPercentFemale());
    }

    @Test
    void testFetchPopulation_WithData() throws Exception {
        List<Map<String, Object>> pyramid = new ArrayList<>();

        pyramid.add(Map.of(
                "age", "0 to 4",
                "male", 50,
                "female", 50
        ));
        pyramid.add(Map.of(
                "age", "5 to 9",
                "male", 40,
                "female", 60
        ));
        pyramid.add(Map.of(
                "age", "85 and over",
                "male", 10,
                "female", 20
        ));

        when(populationRestService.fetchPopulationData(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(pyramid);

        PopulationDemographics result = populationService.fetchPopulation(0, 0, 10);

        assertNotNull(result);
        assertEquals(50+50+40+60+10+20, result.getTotalPopulation(), 0.001);

        // Average age: ((100*2) + (100*7) + (30*85)) / 230 ≈ 13.913
        assertEquals( ((100*2)+(100*7)+(30*85))/230.0 , result.getAvgAge(), 0.001 );

        // Gender percentages
        double malePercent = (50+40+10)/(50+50+40+60+10+20.0)*100.0;
        double femalePercent = (50+60+20)/(50+50+40+60+10+20.0)*100.0;

        assertEquals(malePercent, result.getPercentMale(), 0.001);
        assertEquals(femalePercent, result.getPercentFemale(), 0.001);
    }

    @Test
    void testCalculateAverageAge_EmptyList() {
        double avgAge = populationService.calculateAverageAge(Collections.emptyList());
        assertEquals(0.0, avgAge);
    }

    @Test
    void testCalculateTotalPopulation() {
        List<Map<String, Object>> pyramid = List.of(
                Map.of("age", "0 to 4", "male", 10, "female", 15),
                Map.of("age", "5 to 9", "male", 5, "female", 10)
        );
        double total = populationService.calculateTotalPopulation(pyramid);
        assertEquals(40.0, total, 0.001);
    }

    @Test
    void testCalculateGenderPercentages() {
        List<Map<String, Object>> pyramid = List.of(
                Map.of("age", "0 to 4", "male", 10, "female", 15),
                Map.of("age", "5 to 9", "male", 5, "female", 10)
        );
        Map<String, Double> percentages = populationService.calculateGenderPercentages(pyramid);

        double totalMale = 10 + 5;
        double totalFemale = 15 + 10;
        double total = totalMale + totalFemale;

        assertEquals(totalMale/total*100.0, percentages.get("malePercent"), 0.001);
        assertEquals(totalFemale/total*100.0, percentages.get("femalePercent"), 0.001);
    }
}

