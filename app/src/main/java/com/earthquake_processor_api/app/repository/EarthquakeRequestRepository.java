package com.earthquake_processor_api.app.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.earthquake_processor_api.app.entity.EarthquakeRequest;

@Repository
public interface EarthquakeRequestRepository extends CrudRepository<EarthquakeRequest, Long> {
}
