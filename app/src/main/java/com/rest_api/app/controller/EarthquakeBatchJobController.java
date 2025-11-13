package com.rest_api.app.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EarthquakeBatchJobController {

    private final JobLauncher jobLauncher;
    private final Job earthquakeJob;

    public EarthquakeBatchJobController(JobLauncher jobLauncher, Job earthquakeJob) {
        this.jobLauncher = jobLauncher;
        this.earthquakeJob = earthquakeJob;
    }

    @PostMapping("/batch/earthquakes")
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

