package com.rest_api.app.entity;

import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
public class PopulationDemographics {
    double totalPopulation;
    double avgAge;
    double percentMale;
    double percentFemale;
}
