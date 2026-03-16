# MyOffGridAI Server — Quality Scorecard

**Audit Date:** 2026-03-16T00:39:21Z
**Branch:** main
**Commit:** 0a0f3cad50eb19537fdc07dc161f9f5c2ad23430
**Auditor:** Claude Code (Automated)

---

## Scorecard Summary

| Category             | Score | Max | %    |
|----------------------|-------|-----|------|
| Security             |   12  |  20 |  60% |
| Data Integrity       |   12  |  16 |  75% |
| API Quality          |    8  |  16 |  50% |
| Code Quality         |    0  |  22 |   0% |
| Test Quality         |    0  |  24 |   0% |
| Infrastructure       |    2  |  12 |  17% |
| Snyk Vulnerabilities |    0  |  10 |   0% |
| **OVERALL**          | **34**|**120**|**28%**|

**Grade: F (<40%)**

---

## BLOCKING ISSUES

The following blocking checks caused entire categories to score 0:

1. **CQ-09 — Doc comments on classes ≠ 100%** → 93/110 (85%) — Entire Code Quality = 0
2. **CQ-10 — Doc comments on public methods ≠ 100%** → 154/329 (47%) — Entire Code Quality = 0
3. **CQ-11 — TODO/FIXME/placeholder/stub found** → 2 patterns found — Entire Code Quality = 0
4. **TST-05a/b/c — Test coverage not verified** → JaCoCo not configured; coverage unknown — Entire Test Quality = 0
5. **SNYK-01 — 3 critical dependency vulnerabilities** → Entire Snyk category = 0
6. **SNYK-02 — 17 high dependency vulnerabilities** → Entire Snyk category = 0

---

## Security (10 checks, max 20) — Score: 12/20 (60%)

| Check  | Description                              | Result      | Score | Notes |
|--------|------------------------------------------|-------------|-------|-------|
| SEC-01 | BCrypt/Argon2 password encoding          | 11 hits     | 2/2   | BCryptPasswordEncoder used consistently |
| SEC-02 | JWT signature validation                 | 1 hit       | 2/2   | JwtService validates signatures |
| SEC-03 | SQL injection prevention                 | 0 concat    | 2/2   | No string-concatenated SQL found |
| SEC-04 | CSRF protection                          | 0 (disabled)| 0/2   | CSRF disabled — acceptable for stateless JWT API |
| SEC-05 | Rate limiting configured                 | 0 hits      | 0/2   | **No rate limiting** |
| SEC-06 | Sensitive data logging prevented         | 6 hits      | 0/2   | **Sensitive terms near log statements** |
| SEC-07 | Input validation on endpoints            | 24 @Valid   | 2/2   | Validation present on controllers |
| SEC-08 | Authorization checks on admin endpoints  | 16 hits     | 2/2   | @PreAuthorize used on protected endpoints |
| SEC-09 | Secrets externalized (not in code)       | 0 hardcoded | 2/2   | All secrets use ${} env var interpolation in prod |
| SEC-10 | HTTPS enforced in prod config            | 0 hits      | 0/2   | **No HTTPS/SSL configuration** |

---

## Data Integrity (8 checks, max 16) — Score: 12/16 (75%)

| Check  | Description                              | Result      | Score | Notes |
|--------|------------------------------------------|-------------|-------|-------|
| DI-01  | Entities have audit fields               | 28/35       | 2/2   | Most entities have createdAt/updatedAt |
| DI-02  | Optimistic locking (@Version)            | 0 hits      | 0/2   | **No @Version fields — risk of lost updates** |
| DI-03  | Cascade delete protection                | 0 cascades  | 2/2   | No dangerous cascade deletes |
| DI-04  | Unique constraints defined               | 3 hits      | 2/2   | Unique constraints on key fields |
| DI-05  | Foreign key constraints (JPA)            | 5 rels      | 2/2   | JPA relationships defined |
| DI-06  | Nullable fields documented               | 108 hits    | 2/2   | Extensive @NotNull / nullable annotations |
| DI-07  | Soft delete pattern                      | 0 hits      | 0/2   | **No soft delete — hard deletes only** |
| DI-08  | Transaction boundaries defined           | 44 hits     | 2/2   | @Transactional used across services |

---

## API Quality (8 checks, max 16) — Score: 8/16 (50%)

| Check  | Description                              | Result      | Score | Notes |
|--------|------------------------------------------|-------------|-------|-------|
| API-01 | Consistent error response format         | 20 handlers | 2/2   | GlobalExceptionHandler with @ControllerAdvice |
| API-02 | Pagination on list endpoints             | 102 hits    | 2/2   | Pageable/Page used across controllers |
| API-03 | Validation on request bodies             | 24 @Valid   | 2/2   | Validation on request bodies |
| API-04 | Proper HTTP status codes                 | 209 hits    | 2/2   | ResponseEntity used throughout |
| API-05 | API versioning                           | 0 hits      | 0/2   | **No API versioning (/api/v1/)** |
| API-06 | Request/response logging                 | 0 hits      | 0/2   | **No request/response logging filter** |
| API-07 | HATEOAS/hypermedia (optional)            | 19 hits     | 0/2   | Hits are Link imports, not true HATEOAS |
| API-08 | OpenAPI/Swagger annotations              | 0 hits      | 0/2   | **No @Operation/@Schema annotations** |

---

## Code Quality (11 checks, max 22) — Score: 0/22 (0%) — BLOCKED

**CQ-09, CQ-10, and CQ-11 are BLOCKING. Any failure zeroes the entire category.**

| Check  | Description                              | Result      | Status | Notes |
|--------|------------------------------------------|-------------|--------|-------|
| CQ-01  | Constructor injection (not field)        | 0 field / 40 ctor | PASS | Clean constructor injection via @RequiredArgsConstructor |
| CQ-02  | Lombok usage consistent                  | 0 @Data     | PASS   | No Lombok data annotations (manual builders) |
| CQ-03  | No System.out/printStackTrace            | 0 hits      | PASS   | Clean — no console output |
| CQ-04  | Logging framework used                   | 146 loggers | PASS   | SLF4J via @Slf4j throughout |
| CQ-05  | Constants extracted                      | 230 hits    | PASS   | static final + @Value used |
| CQ-06  | DTOs separate from entities              | 35 ent / 55 DTO | PASS | Clear separation |
| CQ-07  | Service layer exists                     | 42 services | PASS   | Full service layer |
| CQ-08  | Repository layer exists                  | 18 repos    | PASS   | Full repository layer |
| CQ-09  | Doc comments on classes = 100%           | 93/110 (85%) | **FAIL — BLOCKING** | 17 classes missing Javadoc |
| CQ-10  | Doc comments on public methods = 100%    | 154/329 (47%) | **FAIL — BLOCKING** | 175 methods missing Javadoc |
| CQ-11  | No TODO/FIXME/placeholder/stub           | 2 found     | **FAIL — BLOCKING** | UsbResetWatcherService.java:93 TODO; SystemPromptBuilder "placeholder" |

---

## Test Quality (12 checks, max 24) — Score: 0/24 (0%) — BLOCKED

**TST-05a/b/c are BLOCKING. Coverage not verified zeroes the entire category.**

| Check  | Description                              | Result      | Status | Notes |
|--------|------------------------------------------|-------------|--------|-------|
| TST-01 | Unit test files                          | 71 files    | PASS   | Comprehensive unit tests |
| TST-02 | Integration test files                   | 22 files    | PASS   | Integration tests present |
| TST-03 | Real database in ITs                     | 3 hits      | PASS   | Testcontainers PostgreSQL |
| TST-04 | Source-to-test ratio                     | 71/~52 src  | PASS   | >1:1 ratio |
| TST-05a| Unit test coverage = 100%                | NOT RUN     | **FAIL — BLOCKING** | JaCoCo not configured in pom.xml |
| TST-05b| Integration test coverage = 100%         | NOT RUN     | **FAIL — BLOCKING** | JaCoCo not configured in pom.xml |
| TST-05c| Combined coverage = 100%                 | NOT RUN     | **FAIL — BLOCKING** | JaCoCo not configured in pom.xml |
| TST-06 | Test config exists                       | 1 file      | PASS   | application-test.yml present |
| TST-07 | Security tests                           | 127 hits    | PASS   | @WithMockUser / Bearer token tests |
| TST-08 | Auth flow e2e                            | 189 hits    | PASS   | Auth integration tests present |
| TST-09 | DB state verification in ITs             | 51 hits     | PASS   | Repository assertions in ITs |
| TST-10 | Total @Test methods                      | 744 (621+123) | PASS | Substantial test suite |

---

## Infrastructure (6 checks, max 12) — Score: 2/12 (17%)

| Check  | Description                              | Result      | Score | Notes |
|--------|------------------------------------------|-------------|-------|-------|
| INF-01 | Non-root Dockerfile                      | NO          | 0/2   | **No Dockerfile** |
| INF-02 | DB ports localhost only                  | NO          | 0/2   | **No docker-compose.yml** |
| INF-03 | Env vars for prod secrets                | 0 ${} refs  | 0/2   | **No env var interpolation in application-prod.yml** |
| INF-04 | Health check endpoint                    | 22 hits     | 2/2   | Actuator health endpoint enabled |
| INF-05 | Structured logging                       | 0 hits      | 0/2   | **No logback/JsonLayout/LogstashEncoder** |
| INF-06 | CI/CD config                             | 0 files     | 0/2   | **No GitHub Actions, Jenkinsfile, or GitLab CI** |

---

## Snyk Vulnerabilities (5 checks, max 10) — Score: 0/10 (0%) — BLOCKED

**SNYK-01 and SNYK-02 are BLOCKING. Critical/high vulnerabilities zero the entire category.**

| Check   | Description                             | Result      | Status | Notes |
|---------|-----------------------------------------|-------------|--------|-------|
| SNYK-01 | Zero critical dependency vulns          | 3 critical  | **FAIL — BLOCKING** | spring-security-core, spring-security-crypto, tomcat-embed-core |
| SNYK-02 | Zero high dependency vulns              | 17 high     | **FAIL — BLOCKING** | jackson-core, netty, tomcat, postgresql, spring-beans, spring-core, commons-lang3 |
| SNYK-03 | Medium/low dependency vulns             | 15 found    | INFO   | 11 medium + 4 low |
| SNYK-04 | Zero code (SAST) errors                 | UNAVAILABLE | N/A    | Snyk Code scan returned error (code 0005) |
| SNYK-05 | Zero code (SAST) warnings               | UNAVAILABLE | N/A    | Snyk Code scan returned error (code 0005) |

### Critical Vulnerabilities (BLOCKING)

| Package | Version | Vulnerability | Fix |
|---------|---------|---------------|-----|
| org.springframework.security:spring-security-core | 6.4.3 | Missing Authentication for Critical Function | Upgrade to 6.4.6 |
| org.springframework.security:spring-security-crypto | 6.4.3 | Authentication Bypass by Primary Weakness | Upgrade to 6.3.8 |
| org.apache.tomcat.embed:tomcat-embed-core | 10.1.36 | Improper Certificate Validation | Upgrade to 9.0.113 |

### High Vulnerabilities (BLOCKING)

| Package | Version | Vulnerability | Fix |
|---------|---------|---------------|-----|
| com.fasterxml.jackson.core:jackson-core | 2.18.2 | Allocation of Resources Without Limits or Throttling | 2.18.6 |
| io.netty:netty-codec-http2 | 4.1.118.Final | Allocation of Resources / Data Amplification (2 vulns) | 4.1.125.Final |
| io.netty:netty-codec-http | 4.1.118.Final | HTTP Request Smuggling / Data Amplification (2 vulns) | 4.1.125.Final |
| org.apache.commons:commons-lang3 | 3.17.0 | Uncontrolled Recursion | 3.18.0 |
| org.apache.tomcat.embed:tomcat-embed-core | 10.1.36 | 7 vulnerabilities (DoS, Path Traversal, Auth Bypass, etc.) | Various |
| org.postgresql:postgresql | 42.7.5 | Incorrect Auth Algorithm | 42.7.7 |
| org.springframework:spring-beans | 6.2.3 | Relative Path Traversal | 6.2.10 |
| org.springframework:spring-core | 6.2.3 | Incorrect Authorization | 6.2.11 |

---

## Remediation Priority

### P0 — Must Fix (Blocking Scores)

1. **Upgrade Spring Boot to latest 3.4.x** — resolves critical vulns in spring-security, tomcat, spring-core, spring-beans
2. **Add JaCoCo plugin to pom.xml** — enables test coverage measurement (TST-05 blocking)
3. **Add Javadoc to 17 undocumented classes** — resolves CQ-09 blocking
4. **Add Javadoc to 175 undocumented public methods** — resolves CQ-10 blocking
5. **Resolve 2 TODO/placeholder patterns** — resolves CQ-11 blocking
6. **Upgrade transitive dependencies** — jackson-core, netty, commons-lang3, postgresql

### P1 — Should Fix (Category Scores < 60%)

7. **Add rate limiting** (SEC-05) — e.g., Bucket4j or Spring Cloud Gateway
8. **Audit sensitive data logging** (SEC-06) — review 6 log statements near password/token/secret
9. **Configure HTTPS for production** (SEC-10)
10. **Add API versioning** (API-05) — /api/v1/ prefix
11. **Add request/response logging filter** (API-06)
12. **Add OpenAPI annotations** (API-08) — @Operation, @Schema on controllers
13. **Add Dockerfile** (INF-01) — non-root container image
14. **Add docker-compose.yml** (INF-02) — PostgreSQL service
15. **Add structured logging** (INF-05) — LogstashEncoder or JsonLayout
16. **Add CI/CD pipeline** (INF-06) — GitHub Actions

### P2 — Nice to Have

17. **Add @Version for optimistic locking** (DI-02) — on frequently-updated entities
18. **Add soft delete pattern** (DI-07) — for audit-sensitive entities
19. **Consider HATEOAS** (API-07) — for public API consumers

---

**NOTE:** Run `OpenAPI-Template.md` separately to generate the API specification.
