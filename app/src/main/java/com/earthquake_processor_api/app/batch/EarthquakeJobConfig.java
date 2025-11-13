package com.earthquake_processor_api.app.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.earthquake_processor_api.app.entity.EarthquakeEvent;
import com.earthquake_processor_api.app.entity.ProcessedEarthquake;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class EarthquakeJobConfig {

    private final EarthquakeBatchConfig batchConfig;
    private final TruncateEarthquakeEventTasklet truncateEarthquakeEventTasklet;
    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public EarthquakeJobConfig(EarthquakeBatchConfig batchConfig,
                               TruncateEarthquakeEventTasklet truncateEarthquakeEventTasklet,
                               EntityManagerFactory entityManagerFactory,
                               JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        this.batchConfig = batchConfig;
        this.truncateEarthquakeEventTasklet = truncateEarthquakeEventTasklet;
        this.entityManagerFactory = entityManagerFactory;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Step earthquakeStep() {
        return new StepBuilder("earthquakeStep", jobRepository)
                .<EarthquakeEvent, ProcessedEarthquake>chunk(5, transactionManager)
                .reader(batchConfig.earthquakeEventReader(entityManagerFactory))
                .processor(batchConfig.earthquakeEventProcessor())
                .writer(batchConfig.processedEarthquakeWriter(entityManagerFactory))
                .build();
    }

    @Bean
    public Step truncateEarthquakeEventStep() {
        return new StepBuilder("truncateEarthquakeEventStep", jobRepository)
                .tasklet(truncateEarthquakeEventTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job earthquakeJob() {
        return new JobBuilder("earthquakeJob", jobRepository)
                .start(earthquakeStep())
                .next(truncateEarthquakeEventStep())
                .build();
    }
}
