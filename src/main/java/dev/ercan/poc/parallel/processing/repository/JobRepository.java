package dev.ercan.poc.parallel.processing.repository;

import dev.ercan.poc.parallel.processing.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    int countByStatus(Job.Status status);

    @Query(value = "SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<Job> findNextJobStandard();

    @Query(value = "SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Job> findNextJobSkipLocked();

}
