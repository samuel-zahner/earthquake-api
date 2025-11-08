package com.rest_api.app.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.rest_api.app.entity.EarthquakeEvent;

@Repository
public interface EarthquakeEventRepository extends CrudRepository<EarthquakeEvent, Long> {
    
}
