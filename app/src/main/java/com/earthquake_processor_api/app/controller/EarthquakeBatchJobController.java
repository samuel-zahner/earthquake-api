package com.earthquake_processor_api.app.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Earthquake Batch Job Controller", description = "Controller to trigger earthquake batch processing jobs")
public class EarthquakeBatchJobController {

    private final JobLauncher jobLauncher;
    private final Job earthquakeJob;

    public EarthquakeBatchJobController(JobLauncher jobLauncher, Job earthquakeJob) {
        this.jobLauncher = jobLauncher;
        this.earthquakeJob = earthquakeJob;
    }

    @PostMapping("/batch/earthquakes")
    @PreAuthorize("hasRole('user.read')")
    @Operation(summary = "Trigger Earthquake Batch Job", description = "Starts the batch job to process raw earthquake data.")
    @Scheduled(cron="0 0 1 * * *")
    public ResponseEntity<String> runEarthquakeBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // ensures unique run
                    .toJobParameters();

            jobLauncher.run(earthquakeJob, params);

            return ResponseEntity.ok("Earthquake batch job started successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to start earthquake batch job: " + e.getMessage());
        }
    }
}

