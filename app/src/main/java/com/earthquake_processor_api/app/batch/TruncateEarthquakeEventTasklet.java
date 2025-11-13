package com.earthquake_processor_api.app.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class TruncateEarthquakeEventTasklet implements Tasklet {

    private final EntityManagerFactory entityManagerFactory;

    public TruncateEarthquakeEventTasklet(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();

        // Truncate or delete, depending on your DB
        em.createNativeQuery("TRUNCATE TABLE earthquake_event").executeUpdate();

        em.getTransaction().commit();
        em.close();

        return RepeatStatus.FINISHED;
    }
}
