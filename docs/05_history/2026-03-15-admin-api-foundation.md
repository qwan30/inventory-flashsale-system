# 2026-03-15 Admin API Foundation

## What Changed

- the repo now has app-managed admin and operator auth with JWT access and refresh tokens
- campaign lifecycle management is now exposed through secured admin APIs with immutable activity audit trails
- ops endpoints are now treated as authenticated admin or operator surfaces rather than anonymous operational helpers

## Evidence

- `.\mvnw --% -pl apps/api -am test -Dtest=AdminSecurityIntegrationTest,FlashSaleCampaignTest -Dsurefire.failIfNoSpecifiedTests=false` passed
- canonical docs were updated to remove stale statements about reconciliation and missing admin APIs

## How To Reuse This Next Session

- read `docs/03_implementation/2026-03-15-admin-security-and-campaign-slice.md` before building the admin UI or adding more admin workflows
- assume admin and ops work must preserve audit logging and JWT role boundaries from this slice
