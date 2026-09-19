package dev.ercan.poc.parallel.processing.processor;

import dev.ercan.poc.parallel.processing.entity.Job;
import dev.ercan.poc.parallel.processing.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SkipLockedJobProcessor implements JobProcessor {

  private final JobRepository jobRepository;

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public boolean processNextJob() {
    Optional<Job> jobOpt = jobRepository.findNextJobSkipLocked();

    if (jobOpt.isEmpty()) {
      return false;
    }

    Job job = jobOpt.get();
    job.setStatus(Job.Status.COMPLETED);
    jobRepository.save(job);

    return true;
  }

  @Override
  public JobProcessorType getJobProcessorType() {
    return JobProcessorType.SKIP_LOCKED;
  }


}
