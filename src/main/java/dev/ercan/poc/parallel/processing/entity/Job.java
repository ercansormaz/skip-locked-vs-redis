package dev.ercan.poc.parallel.processing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jobs", indexes = {@Index(name = "idx_jobs_status", columnList = "status")})
public class Job {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String payload;

  @Enumerated(EnumType.STRING)
  private Status status;

  public enum Status {
    PENDING,
    PROCESSING,
    COMPLETED
  }
}