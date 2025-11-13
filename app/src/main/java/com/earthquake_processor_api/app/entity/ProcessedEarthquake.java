package com.earthquake_processor_api.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_earthquakes",
       uniqueConstraints = {@UniqueConstraint(columnNames = "earthquake_global_id")})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEarthquake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "earthquake_global_id", nullable = false)
    private String earthquakeGlobalId;

    @Column
    private Double magnitude;

    @Column(name = "mag_type")
    private String magType;

    @Column
    private String place;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "depth")
    private Double depth;

    @Column
    private Boolean tsunami;

    @Column(name = "alert_level")
    private String alertLevel;

    @Column(name = "is_significant")
    private Boolean isSignificant;

    @Column(name = "nearest_city")
    private String nearestCity;

    @Column(name = "distance_to_nearest_city_km")
    private Double distanceToNearestCityKm;

    @Column(name = "population_100km")
    private Double population100km;

    @Column(name = "avg_age_100km")
    private Double avgAge100km;

    @Column(name = "percent_male_100km")
    private Double percentMale100km;

    @Column(name = "percent_female_100km")
    private Double percentFemale100km;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();
}

