package dev.ercan.poc.parallel.processing.controller;

import dev.ercan.poc.parallel.processing.processor.JobProcessorType;
import dev.ercan.poc.parallel.processing.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

  private final SimulationService simulationService;

  @PostMapping("/skip-locked")
  public ResponseEntity<String> skipLocked(@RequestParam int workerCount, @RequestParam int totalJobs) {
    String report = simulationService.runSimulation(JobProcessorType.SKIP_LOCKED, workerCount, totalJobs);
    return ResponseEntity.ok(report);
  }

  @PostMapping("/redis")
  public ResponseEntity<String> redisLock(@RequestParam int workerCount, @RequestParam int totalJobs) {
    String report = simulationService.runSimulation(JobProcessorType.REDIS, workerCount, totalJobs);
    return ResponseEntity.ok(report);
  }

  @PostMapping("/redis-double-check")
  public ResponseEntity<String> redisDoubleCheckedLock(@RequestParam int workerCount, @RequestParam int totalJobs) {
    String report = simulationService.runSimulation(JobProcessorType.REDIS_DOUBLE_CHECKED, workerCount, totalJobs);
    return ResponseEntity.ok(report);
  }

}