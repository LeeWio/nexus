# Nexus Architectural Review & Roadmap

This document outlines the architectural optimizations and future development roadmap for the Nexus CMS project.

## 1. Core Architecture Optimizations (Performance & Clean Code)

### 1.1 Un-intrusive Cache Consistency (Target: High)
*   **Current State:** Manual, imperative cache eviction using `redisUtil.delete()` and `deleteByPattern()` (which blocks Redis).
*   **Optimization Plan:** Implement a non-intrusive cache consistency architecture. Use **Canal** (or Debezium) to listen to MySQL Binlog changes. Send invalidation events via a Message Queue (MQ) to asynchronously clear Redis caches, decoupling business logic from cache management.

### 1.2 High-Concurrency View Sync (Target: High)
*   **Current State:** `PostViewCountSyncTask` syncs Redis views to DB using a non-atomic read-update-clear cycle.
*   **Optimization Plan:** Utilize **Redis Lua Scripts** to atomically read and reset the view increments, eliminating data loss/duplication risks during the sync window.

### 1.3 Search Strategy Decoupling (Target: Medium)
*   **Current State:** Dual maintenance of JPA Criteria and Elasticsearch models.
*   **Optimization Plan:** Introduce a `SearchProvider` interface (Strategy Pattern). Deprecate complex JPA searches in favor of MySQL Full-Text Search for lightweight deployments, or exclusively use ES for enterprise deployments.

### 1.4 Deep Tree Queries (Comment System) (Target: Medium)
*   **Current State:** Potential N+1 query issues for deep hierarchical comments despite EntityGraph usage.
*   **Optimization Plan:** Migrate the comment hierarchy storage to a **Closure Table** or **Path Enumeration** model to enable single-query retrieval of entire comment trees, leveraging Hutool's `TreeUtil` for in-memory assembly.

## 2. Commercial Feature Roadmap (Next Logical Steps)

### 2.1 Block-based Editor (Gutenberg/Notion style)
*   Transition from storing raw HTML to Block-based JSON. Enhances multi-platform rendering (Web, App, Mini-Program) and granular content indexing.

### 2.2 Webhook & Event Hook Ecosystem
*   Expose internal `ApplicationEventPublisher` events to external systems. Allow administrators to register URLs that are triggered upon post publication, comment creation, etc., enabling CI/CD triggers or Chatbot integrations.

### 2.3 Global Media Library Management
*   Extend `FileServiceImpl` to support a centralized media library. Include reference counting, content-based deduplication (SHA-256 hash), and abstraction for S3-compatible cloud storage.

### 2.4 Fine-grained RBAC & Workflows
*   Enhance the security model beyond `ADMIN` and `USER`. Implement approval workflows (Draft -> Review -> Published) with method-level `@PreAuthorize` security checks.