# Nexus Architecture & Roadmap

This document outlines the architectural patterns implemented in the Nexus CMS project, as well as the future development roadmap.

## 1. Implemented Architectural Optimizations (Performance & Clean Code)

### 1.1 Un-intrusive Cache Consistency (Completed)
*   **Architecture:** Implemented a non-intrusive cache consistency architecture. Canal is used to listen to MySQL Binlog changes. Invalidation events are handled via `CanalCacheInvalidationListener` to asynchronously clear Redis caches, completely decoupling business logic from cache management.

### 1.2 High-Concurrency View Sync (Completed)
*   **Architecture:** `PostViewCountSyncTask` syncs Redis views to DB securely. It utilizes **Redis Lua Scripts** (`RedisUtil.hashGetAllAndDelete`) to atomically read and reset the view increments, eliminating data loss or duplication risks during the sync window.

### 1.3 Search Strategy Decoupling (Completed)
*   **Architecture:** Introduced a `SearchProvider` interface strategy. The system supports switching between lightweight MySQL-backed search and Elasticsearch via the `--app.search.type` configuration, decoupling the application from hard dependencies on ES for simpler deployments.

### 1.4 Deep Tree Queries (Comment System) (Completed)
*   **Architecture:** Migrated the comment hierarchy storage to a **Path Enumeration** model. This avoids N+1 query issues and enables single-query retrieval of entire comment trees. Hutool's `TreeUtil` is leveraged for efficient in-memory tree assembly.

## 2. Implemented Commercial Features

### 2.1 Webhook & Event Hook Ecosystem (Completed)
*   **Feature:** Built a comprehensive Webhook ecosystem. Internal `ApplicationEventPublisher` events are exposed to external systems via `WebhookDispatcher`. Administrators can register endpoints triggered upon post publication, comment creation, etc., enabling seamless CI/CD or Chatbot integrations.

### 2.2 Global Media Library Management (Completed)
*   **Feature:** Centralized media library powered by `FileMetadata`. It includes reference counting (`referenceCount`) and content-based deduplication using SHA-256 hashes (`fileHash`), avoiding redundant storage of duplicate uploads.

### 2.3 Fine-grained RBAC & Workflows (Completed)
*   **Feature:** Enhanced security model with dynamic `Role` management and content approval workflows. `PostStatus` includes comprehensive lifecycle states (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `ARCHIVED`) with corresponding `@PreAuthorize` method-level security checks.

## 3. Future Roadmap

### 3.1 Block-based Editor (Gutenberg/Notion style)
*   **Target:** High
*   **Plan:** Transition from storing raw Markdown to Block-based JSON. This will significantly enhance multi-platform rendering (Web, App, Mini-Program) and enable more granular content indexing and modular layout designs.
