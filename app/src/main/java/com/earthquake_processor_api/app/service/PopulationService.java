package com.earthquake_processor_api.app.service;

import org.springframework.stereotype.Service;

import com.earthquake_processor_api.app.entity.PopulationDemographics;

import java.util.*;

@Service
public class PopulationService {

    private final PopulationRestService populationRestService;

    public PopulationService(PopulationRestService populationRestService) {
        this.populationRestService = populationRestService;
    }

    /**
     * Fetch total population within radiusKm of a lat/lng point for a given year
     */
    public PopulationDemographics fetchPopulation(double lat, double lng, double radiusKm) throws Exception {
        List<Map<String, Object>> pyramid = populationRestService.fetchPopulationData(lat, lng, radiusKm);

        if (pyramid.isEmpty()){
            return PopulationDemographics.builder()
                    .build();
        }
        Map<String,Double> genderPercentages = calculateGenderPercentages(pyramid);
        PopulationDemographics populationDemographics = PopulationDemographics.builder()
                                                            .totalPopulation(calculateTotalPopulation(pyramid))
                                                            .avgAge(calculateAverageAge(pyramid))
                                                            .percentMale(genderPercentages.get("malePercent"))
                                                            .percentFemale(genderPercentages.get("femalePercent"))
                                                            .build();
        
        return populationDemographics;
    }

    public double calculateAverageAge(List<Map<String, Object>> pyramid) {
        double totalPopulation = 0;
        double weightedAgeSum = 0;

        for (Map<String, Object> ageGroup : pyramid) {
            String ageRange = (String) ageGroup.get("age");
            double male = ((Number) ageGroup.get("male")).doubleValue();
            double female = ((Number) ageGroup.get("female")).doubleValue();

            // population in this range
            double groupTotal = male + female;

            // approximate midpoint of age range
            double midpoint = parseAgeMidpoint(ageRange);

            weightedAgeSum += groupTotal * midpoint;
            totalPopulation += groupTotal;
        }

        return totalPopulation > 0 ? weightedAgeSum / totalPopulation : 0;
}

    private double parseAgeMidpoint(String ageRange) {
        if (ageRange.contains("and over")) {
            return 85; // assume 85+ = midpoint 85
        }
        String[] parts = ageRange.split("to");
        try {
            double start = Double.parseDouble(parts[0].trim());
            double end = Double.parseDouble(parts[1].trim());
            return (start + end) / 2.0;
        } catch (Exception e) {
            return 0;
        }
    }

    
    public double calculateTotalPopulation(List<Map<String, Object>> pyramid) {
        double total = 0.0;
        for (Map<String, Object> ageGroup : pyramid) {
            double male = ((Number) ageGroup.get("male")).doubleValue();
            double female = ((Number) ageGroup.get("female")).doubleValue();
            total += male + female;
        }
        return total;
    }

    public Map<String, Double> calculateGenderPercentages(List<Map<String, Object>> pyramid) {
        double totalMale = 0.0;
        double totalFemale = 0.0;

        for (Map<String, Object> ageGroup : pyramid) {
            totalMale += ((Number) ageGroup.get("male")).doubleValue();
            totalFemale += ((Number) ageGroup.get("female")).doubleValue();
        }

        double total = totalMale + totalFemale;
        if (total == 0) {
            return Map.of("malePercent", 0.0, "femalePercent", 0.0);
        }

        double malePercent = (totalMale / total) * 100.0;
        double femalePercent = (totalFemale / total) * 100.0;

        return Map.of("malePercent", malePercent, "femalePercent", femalePercent);
    }


}
