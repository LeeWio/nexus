# OPS-STORAGE-001: production deployment storage

Status: READY_FOR_REVIEW locally. Production deployment verification is pending the normal PR and environment gates.

Nexus shares a production disk with Odyssey. An accumulation of historical local deployment images exhausted that disk and prevented Odyssey from creating its temporary Docker config. The shared policy is copied into each self-contained SSH deployment rather than running one repository's mutable script from the other.

Replace the host-wide dangling-image prune with the Nexus-scoped storage check and retention rules in [Release Engineering](../../.github/RELEASE_ENGINEERING.md). See that document for thresholds and protected images. The moved API script preserves immutable image and health verification and pins the prior version before switching. No application code, schema, Docker volume, registry retention policy, or approval rules change.

Validation: Bash syntax and Actionlint pass. An offline Linux Docker fixture covers 79%/80%, separate temporary mounts, disk reporting failures, old commit and digest refs, stopped containers, pinned and incoming refs, cross-project scope, retention limits, and insufficient space after Docker refuses removal. No production image was changed by validation. Revert the workflow changes to disable the local policy; no migration is required.
