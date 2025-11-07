package com.rest_api.app.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.rest_api.app.entity.EarthquakeRequest;

@Repository
public interface EarthquakeRequestRepository extends CrudRepository<EarthquakeRequest, Long> {
}
