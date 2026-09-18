package dev.ercan.poc.parallel.processing.processor;

import dev.ercan.poc.parallel.processing.entity.Job;
import dev.ercan.poc.parallel.processing.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisDoubleCheckedLockJobProcessor implements JobProcessor {

    private final JobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean processNextJob() {
        Optional<Job> jobOpt = jobRepository.findNextJobStandard();
        if (jobOpt.isEmpty()) return false;

        Job job = jobOpt.get();
        String lockKey = "lock:job:" + job.getId();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(30));

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // double check
                Job freshJob = jobRepository.findById(job.getId()).orElse(null);

                if (freshJob == null || !Job.Status.PENDING.equals(freshJob.getStatus())) {
                    return false;
                }

                job.setStatus(Job.Status.COMPLETED);
                jobRepository.save(job);
                return true;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        return false;
    }

    @Override
    public JobProcessorType getJobProcessorType() {
        return JobProcessorType.REDIS_DOUBLE_CHECKED;
    }
}
