# 2026-06-24 CI/CD Pipeline Overhaul

- Overhauled the core CI/CD pipeline (`ci.yml` and `cd.yml`) to support parallel execution of backend/frontend tests and package builds, path-based skipping (`dorny/paths-filter@v3`), and concurrency control to prevent race conditions on master.
- Added a manual rollback workflow (`rollback.yml`) with safety checks, SSH-based deployment to specific container image tags, and automated post-rollback health checks.
- Configured Dependabot (`dependabot.yml`) for weekly automated dependency updates across npm, Maven, and GitHub Actions ecosystems.
- Verified that backend compilation and frontend tests compile clean, and the updated CI pipeline successfully triggers on master branch commits.
