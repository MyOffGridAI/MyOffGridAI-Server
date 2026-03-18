# MyOffGridAI-Server — Quality Scorecard

**Generated:** 2026-03-18
**Branch:** main
**Commit:** 9f2d126 Add centralized file-based logging for server

---

## Security (10 checks, max 20)

| Check | Result | Score |
|-------|--------|-------|
| SEC-01 BCrypt/PasswordEncoder | 22 references | 2/2 |
| SEC-02 JWT signature validation | 5 references | 2/2 |
| SEC-03 No SQL string concatenation | 1 found (AppConstants string literal — false positive) | 2/2 |
| SEC-04 CSRF protection | Disabled (stateless JWT — correct) | 2/2 |
| SEC-05 Rate limiting configured | 5 references (Bucket4j) | 2/2 |
| SEC-06 Sensitive data logging | 12 references to "token" in log context (MDC, not values) | 0/2 |
| SEC-07 Input validation | 34 @Valid/@Validated | 2/2 |
| SEC-08 Authorization checks | 34 @PreAuthorize/hasRole | 2/2 |
| SEC-09 Secrets externalized | 0 hardcoded secrets | 2/2 |
| SEC-10 HTTPS enforced in prod | Not in code (reverse proxy) | 0/2 |

**Security Score: 16 / 20 (80%)**

---

## Data Integrity (8 checks, max 16)

| Check | Result | Score |
|-------|--------|-------|
| DI-01 Entities with audit fields | 35 files with createdAt/updatedAt | 2/2 |
| DI-02 Optimistic locking (@Version) | 0 | 0/2 |
| DI-03 Cascade delete protection | 0 (manual cascade via DataWipeService) | 0/2 |
| DI-04 Unique constraints | 6 | 2/2 |
| DI-05 FK constraints (JPA) | 5 @ManyToOne/@OneToMany | 2/2 |
| DI-06 Nullable docs | 139 nullable=false/@NotNull | 2/2 |
| DI-07 Soft delete | 0 (hard delete — GDPR wipe pattern) | 0/2 |
| DI-08 @Transactional | 73 | 2/2 |

**Data Integrity Score: 10 / 16 (62.5%)**

---

## API Quality (8 checks, max 16)

| Check | Result | Score |
|-------|--------|-------|
| API-01 Error response format | 21 @ExceptionHandler | 2/2 |
| API-02 Pagination | 113 Pageable references | 2/2 |
| API-03 Request validation | 34 @Valid @RequestBody | 2/2 |
| API-04 HTTP status codes | 292 ResponseEntity | 2/2 |
| API-05 API versioning | 0 (no /v1/ prefix) | 0/2 |
| API-06 Request logging | 2 LoggingFilter | 2/2 |
| API-07 HATEOAS | 0 (not applicable) | 0/2 |
| API-08 OpenAPI annotations | 0 (spec generated separately) | 0/2 |

**API Quality Score: 10 / 16 (62.5%)**

---

## Code Quality (11 checks, max 22) — BLOCKED

| Check | Result | Score |
|-------|--------|-------|
| CQ-01 Constructor injection | Field: 2 (@Value), Constructor: 155 | 2/2 |
| CQ-02 Lombok/records consistent | Records + manual (no Lombok) | 2/2 |
| CQ-03 No System.out/printStackTrace | 0 found | 2/2 |
| CQ-04 Logging framework | 220 LoggerFactory | 2/2 |
| CQ-05 Constants extracted | 360 static final/@Value | 2/2 |
| CQ-06 DTOs separate from entities | 84 DTOs | 2/2 |
| CQ-07 Service layer exists | 63 services | 2/2 |
| CQ-08 Repository layer exists | 23 repositories | 2/2 |
| CQ-09 Doc comments on classes (BLOCKING) | **PASS** (172 / 172 = 100%) | 2/2 |
| CQ-10 Doc comments on methods (BLOCKING) | **FAIL** (251 / 556 = 45.1%) | 0/2 |
| CQ-11 No TODO/FIXME/stub (BLOCKING) | **PASS** (0 found) | 2/2 |

**Code Quality Score: 0 / 22 (BLOCKED)**

**BLOCKING: CQ-10 — 305 public methods missing Javadoc (45.1% documented, 100% required)**

---

## Test Quality (12 checks, max 24)

| Check | Result | Score |
|-------|--------|-------|
| TST-01 Unit test files | 113 | 2/2 |
| TST-02 Integration test files | 25 | 2/2 |
| TST-03 Real DB in ITs | 3 (Testcontainers) | 2/2 |
| TST-04 Source-to-test ratio | 113 / 89 (>1:1) | 2/2 |
| TST-05a Unit coverage = 100% (BLOCKING) | Not available (DB required) | ?/2 |
| TST-05b Integration coverage (BLOCKING) | Not available | ?/2 |
| TST-05c Combined coverage (BLOCKING) | Not available | ?/2 |
| TST-06 Test config exists | 1 (application-test.yml) | 2/2 |
| TST-07 Security tests | 148 | 2/2 |
| TST-08 Auth flow e2e | 194 | 2/2 |
| TST-09 DB state in ITs | 68 | 2/2 |
| TST-10 Total @Test methods | 1093 unit + 144 integration = 1237 | 2/2 |

**Test Quality Score: 18 / 24 (75%) — pending TST-05 (coverage requires running DB)**

**Notes:**
- JaCoCo is configured in pom.xml with 100% LINE + BRANCH enforcement (excluding dto/model/*Application)
- Tests could not be run during audit (PostgreSQL unavailable)
- TST-05 checks are BLOCKING — if coverage < 100%, score drops to 0

---

## Infrastructure (6 checks, max 12)

| Check | Result | Score |
|-------|--------|-------|
| INF-01 Non-root Dockerfile | YES (appuser) | 2/2 |
| INF-02 DB ports localhost only | N/A (no docker-compose) | 2/2 |
| INF-03 Env vars for secrets | 24 ${VAR} references | 2/2 |
| INF-04 Health check endpoint | /api/system/status (79 refs) | 2/2 |
| INF-05 Structured logging | LogstashEncoder (10 refs) | 2/2 |
| INF-06 CI/CD config | 0 | 0/2 |

**Infrastructure Score: 10 / 12 (83.3%)**

---

## Snyk Vulnerabilities (5 checks, max 10) — BLOCKED

| Check | Result | Score |
|-------|--------|-------|
| SNYK-01 Zero critical vulns (BLOCKING) | **PASS** (0) | 2/2 |
| SNYK-02 Zero high vulns (BLOCKING) | **FAIL** (1 — tomcat-embed-core) | 0/2 |
| SNYK-03 Medium/low vulns | 1 (logback-core) | 1/2 |
| SNYK-04 Zero SAST errors (BLOCKING) | Snyk Code unavailable | 0/2 |
| SNYK-05 Zero SAST warnings | Snyk Code unavailable | 0/2 |

**Snyk Score: 0 / 10 (BLOCKED)**

**BLOCKING: SNYK-02 — HIGH severity Incorrect Authorization in tomcat-embed-core@10.1.50. Fix: Upgrade spring-boot-starter-web to 3.5.11+**

---

## Scorecard Summary

| Category | Score | Max | % | Status |
|----------|-------|-----|---|--------|
| Security | 16 | 20 | 80.0% | ✅ |
| Data Integrity | 10 | 16 | 62.5% | ⚠ |
| API Quality | 10 | 16 | 62.5% | ⚠ |
| Code Quality | **0** | 22 | **0.0%** | ❌ BLOCKED |
| Test Quality | 18* | 24 | 75.0%* | ⚠ pending |
| Infrastructure | 10 | 12 | 83.3% | ✅ |
| Snyk Vulnerabilities | **0** | 10 | **0.0%** | ❌ BLOCKED |
| **OVERALL** | **64** | **120** | **53.3%** | **D** |

**Overall Grade: D (53.3%)**

*Test Quality is pending TST-05 coverage verification. If coverage < 100%, Test Quality drops to 0 and overall drops to 46/120 (38.3%, Grade F).

---

## Blocking Issues (Must Fix Before Production)

1. **CQ-10 — Public method Javadoc coverage: 45.1% (need 100%)** — 305 undocumented public methods
2. **SNYK-02 — HIGH vulnerability: tomcat-embed-core@10.1.50** — Upgrade Spring Boot parent to 3.5.11+
3. **TST-05 — Test coverage verification pending** — Run `mvn test jacoco:report` with PostgreSQL available

## Recommendations (Non-Blocking)

1. Add `@Version` to frequently-updated entities (DI-02)
2. Consider API versioning for future multi-client support (API-05)
3. Set up CI/CD pipeline — GitHub Actions recommended (INF-06)
4. Document HTTPS enforcement strategy for production deployment (SEC-10)
5. Upgrade Snyk CLI or configure Snyk Code for SAST analysis (SNYK-04/05)
