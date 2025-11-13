package com.rest_api.app.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PopulationDemographics {
    double totalPopulation;
    double avgAge;
    double percentMale;
    double percentFemale;
}
