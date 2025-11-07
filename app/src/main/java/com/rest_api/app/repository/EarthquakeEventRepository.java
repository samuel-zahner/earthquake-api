package com.rest_api.app.repository;

import org.springframework.data.repository.CrudRepository;
import com.rest_api.app.entity.EarthquakeEvent;

public interface EarthquakeEventRepository extends CrudRepository<EarthquakeEvent, Long> {
    
}
