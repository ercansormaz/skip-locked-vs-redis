package dev.ercan.poc.parallel.processing.processor;

public interface JobProcessor {

  boolean processNextJob();

  JobProcessorType getJobProcessorType();

}
