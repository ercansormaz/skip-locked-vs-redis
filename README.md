# Distributed Job Queues: MySQL SKIP LOCKED vs Redis

A Spring Boot application demonstrating and benchmarking different job queue and distributed locking strategies under
high concurrency. This project compares Redis-based locks (with and without Double-Checked Locking) against MySQL's
native `FOR UPDATE SKIP LOCKED` functionality to showcase the impact of lock contention, race conditions, and
infrastructure overhead.

---

## 🚀 Tech Stack

- Java 21
- Spring Boot 4.1.1
- MySQL 8.4
- Redis 8:10
- Docker & Docker Compose

---

## 🧠 The Problem

When multiple thread or worker nodes attempt to fetch and process `PENDING` jobs from a central database simultaneously,
they create
a race condition. If not handled correctly, multiple workers might pick up the exact same job, leading to duplicate
processing.

This project implements three distinct strategies to solve (or attempt to solve) this problem and benchmarks their
performance.

---

## ⚙️ Strategies Compared

### 1. MySQL `SKIP LOCKED` (The Modern RDBMS Approach)

- **How it works:** Uses `SELECT ... FOR UPDATE SKIP LOCKED` with `READ_COMMITTED` isolation. The database engine
  natively
  locks a row for one transaction and immediately serves the next unlocked row to the next concurrent worker.
- **Pros:** Zero external infrastructure, eliminates lock contention, lightning-fast row allocation.

### 2. Naive Redis Lock (The Flawed Approach)

- **How it works:** Workers fetch a job from the DB, then attempt to acquire a lock in Redis (`SETNX`).
- **The Flaw:** Suffers from the "Check-Then-Act" race condition and `@Transactional` commit-timing issues. Workers can
  acquire the lock on a stale read, leading to the same job being processed multiple times.

### 3. Double-Checked Redis Lock (The Safe but Heavy Approach)

- **How it works:** Acquires the Redis lock, then queries the database again to ensure the job is still PENDING before
  processing.
- **Pros/Cons:** Solves the duplicate processing bug, but introduces massive lock contention, network chatter, and
  performance degradation.

---

## 📊 Benchmark Results

Tests were run simulating 10 concurrent workers attempting to process 100 jobs simultaneously.

| Strategy               | Avg. Time (ms) | Success Count | Failed/Skipped Attempts | Conclusion                                                                    |
|------------------------|:--------------:|:-------------:|:-----------------------:|-------------------------------------------------------------------------------|
| SKIP_LOCKED            |     ~214ms     |    100/100    |           ~10           | **Winner**. Blazing fast. The DB routes workers to available jobs instantly.  |
| REDIS (Naive)          |     ~390ms     | 101 - 107 ❌  |         ~1.600          | **Bugged**. Processed more jobs than exist due to transaction commit latency. |
| REDIS (Double-Checked) |     ~580ms     |    100/100    |         ~2.600          | Safe but Slow. Huge network overhead and lock contention.                     |

### 🔍 Key Takeaways

- **The Naive Redis Trap:** Notice how the Naive Redis strategy processed more than 100 jobs. This perfectly illustrates
  the
  danger of releasing a distributed lock in a `finally` block before the Spring `@Transactional` proxy has actually
  committed the database update.
- **Lock Contention:** The Redis Double-Checked strategy generated over 2,500 wasted operations (network trips to Redis
  and
  the DB) just to process 100 jobs. Workers constantly clashed over the same records.
- **The SKIP LOCKED Elegance:** By delegating queue state to MySQL, contention dropped to near-zero. Workers seamlessly
  picked up distinct rows, cutting processing time by more than half compared to Redis.

---

## 🛠️ Getting Started

The entire stack is fully dockerized. You do not need to install Java, MySQL, or Redis on your host machine.

### 1. Spin up the environment

The `docker-compose.yml` file handles a multi-stage build. It compiles the Java code, boots up MySQL and Redis, waits
for
them to be healthy, and then starts the Spring Boot application.

```bash
docker compose up -d
```

*(Wait a few moments for the database to initialize and the application to start on port 8080).*

### 2. Run the Simulations

Trigger the API endpoints using `curl` or Postman to see the benchmarks on your own machine. The endpoints accept
`workerCount` and `totalJobs` as query parameters.

#### Test Strategy 1: MySQL SKIP LOCKED

```bash
curl --request POST --url 'http://localhost:8080/api/simulation/skip-locked?workerCount=10&totalJobs=100'
```

#### Test Strategy 2: Naive Redis (Watch it fail!)

```bash
curl --request POST --url 'http://localhost:8080/api/simulation/redis?workerCount=10&totalJobs=100'
```

#### Test Strategy 3: Double-Checked Redis

```bash
curl --request POST --url 'http://localhost:8080/api/simulation/redis-double-check?workerCount=10&totalJobs=100'
```

### 3. Tearing Down

```bash
docker compose down -v
```
