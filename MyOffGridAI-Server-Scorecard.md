# MyOffGridAI-Server — Quality Scorecard

**Audit Date:** 2026-03-16
**Branch:** main
**Commit:** abec740
**Auditor:** Claude Code (Automated)
**Purpose:** Quantified quality metrics for project health tracking

> This scorecard is NOT loaded into coding sessions — it is for project health tracking only.
> Refer to `MyOffGridAI-Server-Audit.md` for the codebase reference.

---

## Scorecard Summary

| Category             | Score | Max | %     | Status   |
|----------------------|-------|-----|-------|----------|
| Security             |    13 |  20 |  65%  |          |
| Data Integrity       |     9 |  16 |  56%  |          |
| API Quality          |    10 |  16 |  63%  |          |
| Code Quality         |     0 |  22 |   0%  | BLOCKED  |
| Test Quality         |     0 |  24 |   0%  | BLOCKED  |
| Infrastructure       |     7 |  12 |  58%  |          |
| Snyk Vulnerabilities |     0 |  10 |   0%  | BLOCKED  |
| **OVERALL**          | **39**|**120**|**33%**| **Grade: F** |

**Grade Scale:** A (85-100%) | B (70-84%) | C (55-69%) | D (40-54%) | F (<40%)

### Blocking Issues

| Check  | Issue | Impact |
|--------|-------|--------|
| CQ-10  | Public method doc coverage 195/416 (46.9%) — must be 100% | Code Quality category → 0 |
| TST-05a | Unit test coverage 83.2% — must be 100% | Test Quality category → 0 |
| TST-05b | Integration test coverage report missing | Test Quality category → 0 |
| SNYK-01 | 1 critical dependency vulnerability (tomcat-embed-core) | Snyk category → 0 |
| SNYK-02 | 10 high dependency vulnerabilities | Snyk category → 0 |

---

## Security (10 checks, max 20) — Score: 13/20 (65%)

| Check  | Description | Result | Score |
|--------|-------------|--------|-------|
| SEC-01 | BCrypt/Argon2 password encoding | 22 references found | 2/2 |
| SEC-02 | JWT signature validation | 3 references (signWith, parseClaimsJws, validateToken) | 2/2 |
| SEC-03 | SQL injection prevention (no string concat in queries) | **1 potential SQL concatenation found** | 0/2 |
| SEC-04 | CSRF protection | 0 explicit csrf() calls — JWT stateless API (CSRF intentionally disabled) | 1/2 |
| SEC-05 | Rate limiting configured | 2 references (Bucket4j per-IP) | 2/2 |
| SEC-06 | Sensitive data logging prevented | **11 instances of password/secret/token in log statements** | 0/2 |
| SEC-07 | Input validation on all endpoints | 29 @Valid/@Validated annotations on controllers | 2/2 |
| SEC-08 | Authorization checks on admin endpoints | 25 @PreAuthorize/@Secured/hasRole references | 2/2 |
| SEC-09 | Secrets externalized (not hardcoded in config) | 0 hardcoded password strings in yml/properties | 2/2 |
| SEC-10 | HTTPS enforced in prod config | **0 references to require-ssl/server.ssl** | 0/2 |

### Failing Checks
- **SEC-03 (BLOCKING ISSUE):** 1 potential SQL string concatenation found — review for injection risk
- **SEC-06:** 11 log statements reference sensitive keywords (password/secret/token) — ensure values are not logged
- **SEC-10:** No HTTPS enforcement in production configuration

---

## Data Integrity (8 checks, max 16) — Score: 9/16 (56%)

| Check  | Description | Result | Score |
|--------|-------------|--------|-------|
| DI-01 | All entities have audit fields | 34/42 entities with createdAt/updatedAt/@CreatedDate | 1/2 |
| DI-02 | Optimistic locking (@Version) | **0 entities use @Version** | 0/2 |
| DI-03 | Cascade delete protection | 0 CascadeType.ALL/REMOVE (no dangerous cascades) | 2/2 |
| DI-04 | Unique constraints defined | 6 @Column(unique=true)/@UniqueConstraint | 1/2 |
| DI-05 | Foreign key constraints (JPA relationships) | 5 @ManyToOne/@OneToMany/@OneToOne/@ManyToMany | 1/2 |
| DI-06 | Nullable fields documented | 133 nullable=false/@NotNull annotations | 2/2 |
| DI-07 | Soft delete pattern | **0 references to soft delete** | 0/2 |
| DI-08 | Transaction boundaries defined | 66 @Transactional annotations | 2/2 |

### Failing Checks
- **DI-01:** 8 entities missing audit fields (createdAt/updatedAt)
- **DI-02 (BLOCKING ISSUE):** No optimistic locking — concurrent updates may cause silent data loss
- **DI-04:** Only 6 unique constraints across 42 entities — review for missing uniqueness guarantees
- **DI-05:** Only 5 JPA relationship annotations across 22+ entities — review entity relationships
- **DI-07:** No soft delete pattern — hard deletes may cause data loss

---

## API Quality (8 checks, max 16) — Score: 10/16 (63%)

| Check  | Description | Result | Score |
|--------|-------------|--------|-------|
| API-01 | Consistent error response format | 20 @ControllerAdvice/@ExceptionHandler | 2/2 |
| API-02 | Pagination on list endpoints | 112 Pageable/Page references | 2/2 |
| API-03 | Validation on request bodies | 29 @Valid @RequestBody combinations | 2/2 |
| API-04 | Proper HTTP status codes | 266 ResponseEntity/@ResponseStatus | 2/2 |
| API-05 | API versioning | **0 versioned endpoints (/api/v1, etc.)** | 0/2 |
| API-06 | Request/response logging | 2 references (RequestResponseLoggingFilter) | 2/2 |
| API-07 | HATEOAS/hypermedia | 0 Spring HATEOAS usage | 0/2 |
| API-08 | OpenAPI/Swagger annotations | **0 @Operation/@ApiResponse/@Schema annotations** | 0/2 |

### Failing Checks
- **API-05:** No API versioning — breaking changes have no migration path
- **API-07:** No HATEOAS/hypermedia support (optional but recommended)
- **API-08 (BLOCKING ISSUE):** No OpenAPI annotations — API documentation is not generated from code

---

## Code Quality (11 checks, max 22) — Score: 0/22 (BLOCKED)

> **BLOCKED by CQ-10:** Public method documentation coverage is 46.9% (must be 100%). Entire category scores 0.
>
> CQ-09 Doc comments on classes: PASS (143/143 = 100%)
> CQ-10 Doc comments on public methods: FAIL (195/416 = 46.9%)

| Check  | Description | Result | Unblocked Score |
|--------|-------------|--------|-----------------|
| CQ-01 | Constructor injection (not field) | Field: 0, Constructor: 51 | 2/2 |
| CQ-02 | Lombok usage consistent | 0 (not used — project uses manual constructors/records) | 1/2 |
| CQ-03 | No System.out/printStackTrace | 0 violations | 2/2 |
| CQ-04 | Logging framework used | 188 references (SLF4J/LoggerFactory) | 2/2 |
| CQ-05 | Constants extracted | 295 static final/@Value references | 2/2 |
| CQ-06 | DTOs separate from entities | 42 entities, 71 DTOs — clean separation | 2/2 |
| CQ-07 | Service layer exists | 54 service files | 2/2 |
| CQ-08 | Repository layer exists | 23 repository files | 2/2 |
| CQ-09 | Doc comments on classes = 100% (BLOCKING) | **PASS** (143/143 = 100%) | 2/2 |
| CQ-10 | Doc comments on public methods = 100% (BLOCKING) | **FAIL** (195/416 = 46.9%) | 0/2 |
| CQ-11 | No TODO/FIXME/placeholder/stub (BLOCKING) | **PASS** (0 found) | 2/2 |

**Unblocked subtotal:** 19/22 — would score 86% if CQ-10 is resolved

### Blocking Checks
- **CQ-10:** 221 public methods missing Javadoc documentation (excluding DTOs, entities, model classes). Every public method must have doc comments.

---

## Test Quality (12 checks, max 24) — Score: 0/24 (BLOCKED)

> **BLOCKED by TST-05a/b:** Unit test coverage is 83.2% and integration test coverage report is missing (both must be 100%). Entire category scores 0.

| Check  | Description | Result | Unblocked Score |
|--------|-------------|--------|-----------------|
| TST-01 | Unit test files | 94 files | 2/2 |
| TST-02 | Integration test files | 23 files | 2/2 |
| TST-03 | Real database in ITs (Testcontainers) | 3 references (@Testcontainers/@Container/PostgreSQLContainer) | 2/2 |
| TST-04 | Source-to-test ratio | 94 unit tests / 79 source files = 1.19 | 2/2 |
| TST-05a | Unit test coverage = 100% (BLOCKING) | **FAIL** (83.2%) | 0/2 |
| TST-05b | Integration test coverage = 100% (BLOCKING) | **NO REPORT** (no jacoco-it/jacoco.csv) | 0/2 |
| TST-05c | Combined coverage = 100% (BLOCKING) | **NO REPORT** | 0/2 |
| TST-06 | Test config exists | 1 test configuration file | 1/2 |
| TST-07 | Security tests | 139 @WithMockUser/Authorization Bearer references | 2/2 |
| TST-08 | Auth flow e2e | 189 register/login/auth references in ITs | 2/2 |
| TST-09 | DB state verification in ITs | 68 Repository/findBy/count() references in ITs | 2/2 |
| TST-10 | Total @Test methods | 826 unit + 134 integration = 960 total | 2/2 |

**Unblocked subtotal:** 17/24 — would score 71% if TST-05a/b/c are resolved

### Blocking Checks
- **TST-05a:** Unit test coverage at 83.2% — must reach 100%
- **TST-05b:** Integration test JaCoCo report not generated — configure failsafe/jacoco-it plugin
- **TST-05c:** Combined coverage cannot be calculated without both reports

---

## Infrastructure (6 checks, max 12) — Score: 7/12 (58%)

| Check  | Description | Result | Score |
|--------|-------------|--------|-------|
| INF-01 | Non-root Dockerfile | YES (USER directive present) | 2/2 |
| INF-02 | DB ports localhost only | N/A (no docker-compose.yml) | 1/2 |
| INF-03 | Env vars for prod secrets | **0 `${...}` references in application-prod.yml** | 0/2 |
| INF-04 | Health check endpoint | 27 health/actuator references | 2/2 |
| INF-05 | Structured logging | 6 references (LogstashEncoder/logback) | 2/2 |
| INF-06 | CI/CD config | **0 CI/CD configuration files** | 0/2 |

### Failing Checks
- **INF-03 (BLOCKING ISSUE):** Production config does not externalize secrets via environment variables
- **INF-06:** No CI/CD pipeline configured (no GitHub Actions, Jenkins, or GitLab CI)

---

## Snyk Vulnerabilities (5 checks, max 10) — Score: 0/10 (BLOCKED)

> **BLOCKED by SNYK-01 and SNYK-02:** Critical and high dependency vulnerabilities found. Entire category scores 0.

| Check  | Description | Result | Unblocked Score |
|--------|-------------|--------|-----------------|
| SNYK-01 | Zero critical dependency vulnerabilities (BLOCKING) | **FAIL** — 1 critical (tomcat-embed-core) | 0/2 |
| SNYK-02 | Zero high dependency vulnerabilities (BLOCKING) | **FAIL** — 10 high (tomcat, spring-security, spring-beans, spring-core) | 0/2 |
| SNYK-03 | Medium/low dependency vulnerabilities | 7 medium + 3 low = 10 | 1/2 |
| SNYK-04 | Zero code (SAST) errors (BLOCKING) | **SKIPPED** — Snyk Code not enabled for organization | 0/2 |
| SNYK-05 | Zero code (SAST) warnings | **SKIPPED** — Snyk Code not enabled for organization | 0/2 |

**Unblocked subtotal:** 1/10

### Critical Vulnerability Details

| Severity | Package | Vulnerability | Upgrade Path |
|----------|---------|---------------|-------------|
| CRITICAL | org.apache.tomcat.embed:tomcat-embed-core | Via spring-boot-starter-webflux | Upgrade Spring Boot |
| HIGH (x10) | tomcat, spring-security-core, spring-beans, spring-core | Multiple CVEs across Spring ecosystem | Upgrade to Spring Boot 3.4.7+ / Spring Security 6.4.10 |
| MEDIUM | io.netty:netty-codec-http@4.1.125.Final | CRLF Injection (SNYK-JAVA-IONETTY-14423947) | Upgrade reactor-netty-http |
| LOW | io.projectreactor.netty:reactor-netty-http@1.2.6 | System info exposure (SNYK-JAVA-IOPROJECTREACTORNETTY-10770514) | Upgrade reactor-netty-http |

### Recommended Fix
```xml
<!-- Upgrade spring-security-test to 6.4.10 to fix Incorrect Authorization (HIGH) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <version>6.4.10</version>
    <scope>test</scope>
</dependency>
```

---

## Priority Remediation Plan

### P0 — Blocking (must fix to unblock scorecard categories)

1. **CQ-10: Add Javadoc to 221 public methods** — unblocks Code Quality (potential: 19/22 = 86%)
2. **TST-05a: Increase unit test coverage from 83.2% to 100%** — unblocks Test Quality (potential: 17/24 = 71%)
3. **TST-05b: Configure JaCoCo for integration tests** — required for combined coverage metric
4. **SNYK-01/02: Upgrade Spring Boot to resolve critical/high CVEs** — unblocks Snyk category

### P1 — High Priority

5. **SEC-03:** Review and fix potential SQL injection concatenation
6. **SEC-06:** Audit 11 log statements referencing sensitive data — ensure values are masked
7. **INF-03:** Externalize production secrets via `${ENV_VAR}` patterns
8. **DI-02:** Add @Version for optimistic locking on frequently-updated entities

### P2 — Medium Priority

9. **SEC-10:** Configure HTTPS enforcement in production
10. **API-05:** Implement API versioning (/api/v1/)
11. **API-08:** Add OpenAPI/Swagger annotations for API documentation
12. **INF-06:** Add CI/CD pipeline (GitHub Actions)
13. **DI-01:** Add audit fields to remaining 8 entities

### P3 — Low Priority

14. **DI-07:** Consider soft delete pattern for critical entities
15. **API-07:** Consider HATEOAS for API discoverability

---

**If all blocking issues were resolved, the projected score would be:**

| Category             | Projected | Max | %     |
|----------------------|-----------|-----|-------|
| Security             |        13 |  20 |  65%  |
| Data Integrity       |         9 |  16 |  56%  |
| API Quality          |        10 |  16 |  63%  |
| Code Quality         |        19 |  22 |  86%  |
| Test Quality         |        17 |  24 |  71%  |
| Infrastructure       |         7 |  12 |  58%  |
| Snyk Vulnerabilities |         1 |  10 |  10%  |
| **Projected OVERALL**|    **76** |**120**|**63%**| **Grade: C** |
