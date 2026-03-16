# MyOffGridAI-Server — Quality Scorecard

**Audit Date:** 2026-03-16
**Branch:** main
**Commit:** 0722170
**Auditor:** Claude Code (Automated)

---

## Security (10 checks, max 20)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| SEC-01 | BCrypt/Argon2 password encoding | 22 refs | 2/2 | BCryptPasswordEncoder used throughout auth |
| SEC-02 | JWT signature validation | 3 refs | 2/2 | signWith, parseClaimsJws, validateToken present |
| SEC-03 | SQL injection prevention (no string concat) | 1 found | 2/2 | False positive: JPQL annotation string concatenation in EbookRepository (compile-time, parameterized) |
| SEC-04 | CSRF protection | 0 (grep miss) | 2/2 | CSRF disabled via lambda DSL `csrf(csrf -> csrf.disable())` — correct for stateless JWT API |
| SEC-05 | Rate limiting configured | 6 refs | 2/2 | Bucket4j per-IP rate limiting with auth/general tiers |
| SEC-06 | Sensitive data logging prevented | 12 refs | 0/2 | 12 instances of password/secret/token near log statements |
| SEC-07 | Input validation on endpoints | 31 refs | 2/2 | @Valid/@Validated on controller method params |
| SEC-08 | Authorization checks on admin endpoints | 30 refs | 2/2 | @PreAuthorize/hasRole/hasAuthority annotations |
| SEC-09 | Secrets externalized (not in code) | 0 hardcoded | 2/2 | All secrets via ${} environment variables |
| SEC-10 | HTTPS enforced in prod | 0 refs | 0/2 | No require-ssl or server.ssl configuration |

**Security Score: 16 / 20 (80%)**

### Failing Checks:
- **SEC-06** (0/2): 12 potential sensitive-data-in-logs occurrences found (password/secret/token near `log.` calls). Review and sanitize.
- **SEC-10** (0/2): No HTTPS/SSL enforcement in production configuration. Add `server.ssl` or reverse proxy SSL termination config.

---

## Data Integrity (8 checks, max 16)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| DI-01 | All entities have audit fields | 35 / 42 | 1/2 | 83% — 7 entities missing createdAt/updatedAt |
| DI-02 | Optimistic locking (@Version) | 0 refs | 0/2 | No optimistic locking on any entity |
| DI-03 | Cascade delete protection | 0 dangerous | 2/2 | No CascadeType.ALL/REMOVE — safe from accidental cascade deletes |
| DI-04 | Unique constraints defined | 6 refs | 2/2 | @Column(unique=true) and @UniqueConstraint present |
| DI-05 | Foreign key constraints (JPA) | 5 refs | 2/2 | @ManyToOne/@OneToMany relationships defined |
| DI-06 | Nullable fields documented | 134 refs | 2/2 | Extensive nullable=false and @NotNull annotations |
| DI-07 | Soft delete pattern | 0 refs | 0/2 | No soft delete pattern (hard deletes only) |
| DI-08 | Transaction boundaries | 70 refs | 2/2 | @Transactional well-distributed across service layer |

**Data Integrity Score: 11 / 16 (69%)**

### Failing Checks:
- **DI-01** (1/2): 7 entities missing audit fields (createdAt/updatedAt). Add @CreatedDate/@LastModifiedDate.
- **DI-02** (0/2): No @Version field on any entity. Concurrent updates may cause silent data overwrites.
- **DI-07** (0/2): No soft delete pattern. Deleted records are unrecoverable without database backups.

---

## API Quality (8 checks, max 16)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| API-01 | Consistent error response format | 20 refs | 2/2 | GlobalExceptionHandler + @ControllerAdvice |
| API-02 | Pagination on list endpoints | 113 refs | 2/2 | Pageable/Page<>/PageRequest throughout |
| API-03 | Validation on request bodies | 31 refs | 2/2 | @Valid @RequestBody on controller params |
| API-04 | Proper HTTP status codes | 274 refs | 2/2 | ResponseEntity + @ResponseStatus used consistently |
| API-05 | API versioning | 0 refs | 0/2 | No /api/v1/ or versioning scheme |
| API-06 | Request/response logging | 2 refs | 2/2 | RequestResponseLoggingFilter + AuditAspect |
| API-07 | HATEOAS/hypermedia | 0 (actual) | 0/2 | Not implemented — project uses ApiResponse wrapper |
| API-08 | OpenAPI/Swagger annotations | 0 refs | 0/2 | No @Operation/@ApiResponse/@Schema annotations |

**API Quality Score: 10 / 16 (63%)**

### Failing Checks:
- **API-05** (0/2): No API versioning. Breaking changes will affect all clients simultaneously.
- **API-07** (0/2): No HATEOAS/hypermedia links. Clients must hardcode endpoint paths.
- **API-08** (0/2): No OpenAPI annotations. Swagger UI unavailable for API exploration.

---

## Code Quality (11 checks, max 22)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| CQ-01 | Constructor injection | 0 field / yes ctor | 2/2 | Pure constructor injection via @RequiredArgsConstructor |
| CQ-02 | Lombok usage consistent | 0 refs | 2/2 | Consistently NOT used — Java records + manual constructors |
| CQ-03 | No System.out/printStackTrace | 0 refs | 2/2 | Clean — all output via SLF4J logging |
| CQ-04 | Logging framework used | 200 refs | 2/2 | @Slf4j / LoggerFactory throughout |
| CQ-05 | Constants extracted | 325 refs | 2/2 | AppConstants (528+ constants) + @Value injection |
| CQ-06 | DTOs separate from entities | 42 entities / 79 DTOs | 2/2 | Clean separation of concerns |
| CQ-07 | Service layer exists | 59 services | 2/2 | Comprehensive service layer |
| CQ-08 | Repository layer exists | 23 repos | 2/2 | Spring Data JPA repository layer |
| CQ-09 | Doc comments on classes/modules = 100% (BLOCKING) | 151 / 151 | 2/2 | PASS — 100% class documentation |
| CQ-10 | Doc comments on public methods/functions = 100% (BLOCKING) | 211 / 460 | 0/2 | FAIL — 46% method documentation |
| CQ-11 | No TODO/FIXME/placeholder (BLOCKING) | 0 found | 2/2 | PASS — no incomplete code patterns |

**Code Quality Score: 0 / 22 (0%) — BLOCKED**

- CQ-09 Doc comments on classes: 151/151 (100%) — PASS
- CQ-10 Doc comments on public methods: 211/460 (46%) — FAIL (BLOCKING)

> **BLOCKING: CQ-10 FAIL** — Public method documentation at 46% (211/460). The entire Code Quality category scores 0 until all public methods (excluding DTOs, entities, generated code) have Javadoc comments. 249 undocumented public methods must be addressed.

---

## Test Quality (12 checks, max 24)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| TST-01 | Unit test files | 123 files | 2/2 | Comprehensive unit test suite |
| TST-02 | Integration test files | 23 files | 2/2 | IT coverage across features |
| TST-03 | Real database in ITs | 3 refs | 2/2 | Testcontainers + PostgreSQLContainer |
| TST-04 | Source-to-test ratio | 123 / 86 | 2/2 | 1.4:1 test-to-source ratio |
| TST-05a | Unit test coverage = 100% (BLOCKING) | 82.6% | 0/2 | FAIL — 17.4% gap |
| TST-05b | Integration test coverage = 100% (BLOCKING) | N/A | 0/2 | No IT JaCoCo report (no failsafe/jacoco-it config) |
| TST-05c | Combined coverage = 100% (BLOCKING) | 82.6% | 0/2 | FAIL — 17.4% gap |
| TST-06 | Test config exists | 1 file | 2/2 | application-test.yml present |
| TST-07 | Security tests | 141 refs | 2/2 | @WithMockUser + Bearer token tests |
| TST-08 | Auth flow e2e | 189 refs | 2/2 | Register/login/auth IT coverage |
| TST-09 | DB state verification in ITs | 68 refs | 2/2 | Repository/findBy/count() assertions in ITs |
| TST-10 | Total @Test methods | 1,093 | 2/2 | Extensive test method count |

**Test Quality Score: 0 / 24 (0%) — BLOCKED**

> **BLOCKING: TST-05a/b/c FAIL** — Unit test coverage at 82.6% (must be 100%). No integration test JaCoCo report. The entire Test Quality category scores 0 until all coverage metrics reach 100%.

---

## Infrastructure (6 checks, max 12)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| INF-01 | Non-root Dockerfile | YES | 2/2 | USER directive + addgroup/adduser in Dockerfile |
| INF-02 | DB ports localhost only | N/A | 0/2 | No docker-compose.yml — cannot verify port binding |
| INF-03 | Env vars for prod secrets | 9 env vars | 2/2 | application-prod.yml uses ${} for all secrets |
| INF-04 | Health check endpoint | 28 refs | 2/2 | Spring Actuator health configured |
| INF-05 | Structured logging | 6 refs | 2/2 | LogstashEncoder in logback-spring.xml |
| INF-06 | CI/CD config | 0 files | 0/2 | No GitHub Actions / Jenkins / GitLab CI pipeline |

**Infrastructure Score: 8 / 12 (67%)**

### Failing Checks:
- **INF-02** (0/2): No docker-compose.yml to verify database port binding. Add docker-compose for local development.
- **INF-06** (0/2): No CI/CD pipeline. Builds, tests, and deployments are manual.

---

## Security Vulnerabilities — Snyk (5 checks, max 10)

| Check | Description | Raw Result | Score | Notes |
|-------|-------------|-----------|-------|-------|
| SNYK-01 | Zero critical dependency vulns (BLOCKING) | 1 critical | 0/2 | FAIL — tomcat-embed-core Improper Certificate Validation |
| SNYK-02 | Zero high dependency vulns (BLOCKING) | 10 high | 0/2 | FAIL — tomcat-embed-core (7), spring-beans (1), spring-core (1), spring-security-core (1) |
| SNYK-03 | Medium/low dependency vulns | 10 found | 1/2 | 7 medium + 3 low across logback, netty, poi, tomcat, spring-web |
| SNYK-04 | Zero code (SAST) errors (BLOCKING) | N/A | 0/2 | Snyk Code scan unavailable (error code 2) |
| SNYK-05 | Zero code (SAST) warnings | N/A | 0/2 | Snyk Code scan unavailable (error code 2) |

**Snyk Score: 0 / 10 (0%) — BLOCKED**

> **BLOCKING: SNYK-01 FAIL** — 1 critical vulnerability: `tomcat-embed-core@10.1.41` Improper Certificate Validation (fix: upgrade to 9.0.113+)
> **BLOCKING: SNYK-02 FAIL** — 10 high vulnerabilities across tomcat-embed-core, spring-beans, spring-core, spring-security-core

### Full Vulnerability Inventory:
| Severity | Package | Vulnerability | Fix |
|----------|---------|--------------|-----|
| CRITICAL | tomcat-embed-core@10.1.41 | Improper Certificate Validation | 9.0.113 |
| HIGH | tomcat-embed-core@10.1.41 | Allocation of Resources Without Limits (x2) | 9.0.106, 9.0.107 |
| HIGH | tomcat-embed-core@10.1.41 | Integer Overflow or Wraparound | 9.0.107 |
| HIGH | tomcat-embed-core@10.1.41 | Improper Resource Shutdown or Release | 9.0.108 |
| HIGH | tomcat-embed-core@10.1.41 | Relative Path Traversal | 9.0.109 |
| HIGH | tomcat-embed-core@10.1.41 | Untrusted Search Path | 9.0.106 |
| HIGH | tomcat-embed-core@10.1.41 | Incorrect Authorization | 9.0.114 |
| HIGH | spring-beans@6.2.7 | Relative Path Traversal | 6.2.10 |
| HIGH | spring-core@6.2.7 | Incorrect Authorization | 6.2.11 |
| HIGH | spring-security-core@6.4.6 | Incorrect Authorization | 6.4.10 |
| MEDIUM | logback-core@1.5.18 | External Initialization of Trusted Variables | 1.3.16 |
| MEDIUM | netty-codec-http@4.1.125.Final | CRLF Injection | 4.1.129.Final |
| MEDIUM | poi-ooxml@5.3.0 | Improper Input Validation | 5.4.0 |
| MEDIUM | tomcat-embed-core@10.1.41 | Auth Bypass, Session Fixation, Improper Resource Shutdown (x3) | 9.0.106–9.0.110 |
| MEDIUM | spring-web@6.2.7 | HTTP Response Splitting | 6.1.21 |
| LOW | logback-core@1.5.18 | External Initialization of Trusted Variables | 1.5.25 |
| LOW | reactor-netty-http@1.2.6 | Exposure of Sensitive System Info | 1.2.8 |
| LOW | tomcat-embed-core@10.1.41 | Improper Authorization | 9.0.113 |

**Primary Fix:** Upgrade Spring Boot to latest patch release (resolves tomcat-embed-core, spring-beans, spring-core, spring-security-core).

---

## Scorecard Summary

| Category | Score | Max | % | Status |
|----------|-------|-----|---|--------|
| Security | 16 | 20 | 80% | B |
| Data Integrity | 11 | 16 | 69% | C |
| API Quality | 10 | 16 | 63% | C |
| Code Quality | 0 | 22 | 0% | **BLOCKED** |
| Test Quality | 0 | 24 | 0% | **BLOCKED** |
| Infrastructure | 8 | 12 | 67% | C |
| Snyk Vulnerabilities | 0 | 10 | 0% | **BLOCKED** |
| **OVERALL** | **45** | **120** | **38%** | **F** |

**Grade: F (38%)**

> Grade scale: A (85-100%) | B (70-84%) | C (55-69%) | D (40-54%) | F (<40%)

---

## Blocking Issues Summary

These must be resolved before the project can achieve a passing grade:

1. **CQ-10 — Public method documentation (46%)**: 249 undocumented public methods. Add Javadoc to all public methods in service, controller, config, security, and utility classes (excluding DTOs, entities, generated code).

2. **TST-05a — Unit test coverage (82.6%)**: 17.4% coverage gap. Increase JaCoCo line coverage to 100% across all packages.

3. **TST-05b — Integration test coverage (N/A)**: No failsafe/jacoco-it JaCoCo configuration. Add Maven Failsafe plugin with JaCoCo integration test report.

4. **SNYK-01 — Critical vulnerability (1)**: `tomcat-embed-core@10.1.41` has critical certificate validation flaw. Upgrade Spring Boot.

5. **SNYK-02 — High vulnerabilities (10)**: 10 high-severity CVEs across Spring framework and Tomcat. Upgrade Spring Boot to latest patch release.

---

## Non-Blocking Improvement Areas

| Priority | Item | Impact |
|----------|------|--------|
| High | Upgrade Spring Boot (resolves 18/21 Snyk vulns) | Unblocks Snyk category |
| High | Add Javadoc to 249 public methods | Unblocks Code Quality |
| High | Increase test coverage to 100% | Unblocks Test Quality |
| Medium | Add @Version to entities (DI-02) | Prevents concurrent update conflicts |
| Medium | Add API versioning (API-05) | Enables backward-compatible API evolution |
| Medium | Add OpenAPI annotations (API-08) | Enables Swagger UI auto-documentation |
| Medium | Add CI/CD pipeline (INF-06) | Automates build/test/deploy |
| Medium | Configure HTTPS in prod (SEC-10) | Encrypts data in transit |
| Low | Add soft delete pattern (DI-07) | Enables data recovery |
| Low | Add docker-compose.yml (INF-02) | Standardizes local dev environment |
| Low | Audit sensitive data in logs (SEC-06) | Prevents credential leakage |

---

*Generated by Claude Code (Automated) — 2026-03-16*
