package com.rest_api.app.batch;

import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rest_api.app.entity.EarthquakeEvent;
import com.rest_api.app.entity.PopulationDemographics;
import com.rest_api.app.entity.ProcessedEarthquake;
import com.rest_api.app.service.EarthquakeService;
import com.rest_api.app.service.PopulationService;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class EarthquakeBatchConfig {

    private final EarthquakeService earthquakeService;

    private final PopulationService populationService;

    EarthquakeBatchConfig(PopulationService populationService, EarthquakeService earthquakeService) {
        this.populationService = populationService;
        this.earthquakeService = earthquakeService;
    }

    @Bean
    public ItemReader<EarthquakeEvent> earthquakeEventReader(EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<EarthquakeEvent>()
            .name("earthquakeEventReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT e FROM EarthquakeEvent e")
            .pageSize(50)
            .build();
    }

    @Bean
    public ItemProcessor<EarthquakeEvent, ProcessedEarthquake> earthquakeEventProcessor() {
        return (EarthquakeEvent item) -> {
        
        //extract needed info from raw earthquake event
        ProcessedEarthquake processed = new ProcessedEarthquake();
        processed.setEarthquakeGlobalId(item.getEarthquakeGlobalId());
        processed.setMagnitude(item.getMagnitude());
        processed.setMagType(item.getMagType());
        processed.setPlace(item.getPlace());
        processed.setLatitude(item.getLatitude());
        processed.setLongitude(item.getLongitude());
        processed.setDepth(item.getDepth());
        processed.setEventTime(item.getTime().toLocalDateTime());
        processed.setTsunami(item.getTsunami());
        processed.setAlertLevel(item.getAlert());

        //process placement info, to extract nearest city and distance
        processed.setNearestCity(earthquakeService.extractNearestCity(processed.getPlace()));
        processed.setDistanceToNearestCityKm(earthquakeService.extractDistanceToNearestCityKm(processed.getPlace()));

        //enrich with population data
        PopulationDemographics populationDemographics = populationService.fetchPopulation(item.getLatitude(), item.getLongitude(), 100);
        processed.setPopulation100km(populationDemographics.getTotalPopulation());
        processed.setAvgAge100km(populationDemographics.getAvgAge());
        processed.setPercentMale100km(populationDemographics.getPercentMale());
        processed.setPercentFemale100km(populationDemographics.getPercentFemale());

        //determine if significant 
        processed.setIsSignificant(earthquakeService.isSignificantEarthquake(processed.getMagnitude(),
                                                                             processed.getTsunami(),
                                                                             processed.getAlertLevel(),
                                                                             processed.getPopulation100km(),
                                                                             processed.getDistanceToNearestCityKm()));
        
        return processed;
        };
    }

    @Bean
    public ItemWriter<ProcessedEarthquake> processedEarthquakeWriter(EntityManagerFactory emf) {
        JpaItemWriter<ProcessedEarthquake> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }

}

