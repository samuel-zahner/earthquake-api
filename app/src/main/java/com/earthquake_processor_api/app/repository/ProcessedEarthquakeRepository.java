package com.earthquake_processor_api.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.earthquake_processor_api.app.entity.ProcessedEarthquake;

import java.util.Optional;

@Repository
public interface ProcessedEarthquakeRepository extends JpaRepository<ProcessedEarthquake, Long> {

    Optional<ProcessedEarthquake> findByEarthquakeGlobalId(String earthquakeGlobalId);
}
