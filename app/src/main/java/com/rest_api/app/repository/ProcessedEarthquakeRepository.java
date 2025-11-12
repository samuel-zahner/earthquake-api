package com.rest_api.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rest_api.app.entity.ProcessedEarthquake;

import java.util.Optional;

@Repository
public interface ProcessedEarthquakeRepository extends JpaRepository<ProcessedEarthquake, Long> {

    Optional<ProcessedEarthquake> findByEarthquakeGlobalId(String earthquakeGlobalId);
}
