package dev.ercan.poc.parallel.processing.processor;

public enum JobProcessorType {

  REDIS,
  REDIS_DOUBLE_CHECKED,
  SKIP_LOCKED,

  ;

}
