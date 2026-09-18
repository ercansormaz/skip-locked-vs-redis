package dev.ercan.poc.parallel.processing.service;

import dev.ercan.poc.parallel.processing.entity.Job;
import dev.ercan.poc.parallel.processing.processor.JobProcessor;
import dev.ercan.poc.parallel.processing.processor.JobProcessorType;
import dev.ercan.poc.parallel.processing.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final JobRepository jobRepository;
    private final List<JobProcessor> processors;

    public String runSimulation(JobProcessorType processorType, int workerCount, int totalJobs) {
        JobProcessor processor = processors.stream()
                .filter(p -> processorType.equals(p.getJobProcessorType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No processor found for type " + processorType.name()));

        prepareDummyJobs(totalJobs);

        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedAttemptCount = new AtomicInteger(0);

        for (int i = 0; i < workerCount; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                boolean hasMoreJobs = true;
                while (hasMoreJobs) {
                    boolean success = processor.processNextJob();
                    if (success) {
                        processedCount.incrementAndGet();
                    } else {
                        failedAttemptCount.incrementAndGet();

                        if (jobRepository.countByStatus(Job.Status.PENDING) == 0) {
                            hasMoreJobs = false;
                        }
                    }
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        return String.format(
                "Type: %s | Total Job: %d | Worker: %d | Time: %d ms | Success: %d | Skipped: %d",
                processorType.name(), totalJobs, workerCount, durationMs, processedCount.get(), failedAttemptCount.get()
        );
    }

    private void prepareDummyJobs(int count) {
        jobRepository.deleteAll();
        List<Job> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Job job = new Job();
            job.setStatus(Job.Status.PENDING);
            job.setPayload("Test Data " + i);
            jobs.add(job);
        }
        jobRepository.saveAll(jobs);
    }

}
