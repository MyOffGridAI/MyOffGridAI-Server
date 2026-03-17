# MyOffGridAI-Server — Quality Scorecard

**Scorecard Date:** 2026-03-17T12:29:53Z
**Branch:** main
**Commit:** 5305618cb32297d02fa5451c5995f7735a465ca7

---

## Security (10 checks, max 20)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| SEC-01 BCrypt/Argon2 password encoding | 22 references | 2 | PasswordEncoder present and used |
| SEC-02 JWT signature validation | 3 references | 2 | signWith, validateToken present |
| SEC-03 SQL injection prevention | 1 string concat in queries | 1 | 1 potential issue found — needs manual review |
| SEC-04 CSRF protection | 0 (disabled) | 2 | Disabled intentionally — stateless JWT API |
| SEC-05 Rate limiting configured | 10 references | 2 | Bucket4j with 2 tiers |
| SEC-06 Sensitive data logging prevented | 12 references | 1 | Some token/password mentions near log calls |
| SEC-07 Input validation on all endpoints | 32 @Valid/@Validated | 2 | Present on controllers |
| SEC-08 Authorization checks on admin endpoints | 32 @PreAuthorize/hasRole | 2 | Method-level security enabled |
| SEC-09 Secrets externalized (not in code) | 0 hardcoded | 2 | All secrets via env vars in prod |
| SEC-10 HTTPS enforced in prod config | 0 references | 0 | No SSL config found |
| **TOTAL** | | **16** | **/ 20 (80%)** |

---

## Data Integrity (8 checks, max 16)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| DI-01 All entities have audit fields | 35 / 42 | 1 | Most entities have createdAt/updatedAt |
| DI-02 Optimistic locking (@Version) | 0 | 0 | No @Version on any entity |
| DI-03 Cascade delete protection | 0 CascadeType.ALL/REMOVE | 2 | No dangerous cascade rules (explicit delete methods used) |
| DI-04 Unique constraints defined | 6 | 2 | Key fields constrained (username, email, portPath, etc.) |
| DI-05 Foreign key constraints (JPA relationships) | 5 | 1 | Limited JPA relationship usage; most use UUID foreign keys |
| DI-06 Nullable fields documented | 134 references | 2 | Extensively annotated |
| DI-07 Soft delete pattern | 0 | 0 | Hard deletes only |
| DI-08 Transaction boundaries defined | 70 @Transactional | 2 | Widely used on write operations |
| **TOTAL** | | **10** | **/ 16 (63%)** |

---

## API Quality (8 checks, max 16)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| API-01 Consistent error response format | 20 references | 2 | GlobalExceptionHandler + ApiResponse |
| API-02 Pagination on list endpoints | 113 references | 2 | Pageable widely used |
| API-03 Validation on request bodies | 32 @Valid @RequestBody | 2 | Present on controllers |
| API-04 Proper HTTP status codes | 274 ResponseEntity/status | 2 | Comprehensive status code usage |
| API-05 API versioning | 0 | 0 | No /api/v1/ versioning |
| API-06 Request/response logging | 2 references | 2 | RequestResponseLoggingFilter |
| API-07 HATEOAS/hypermedia (optional) | 20 references | 0 | False positive — Link references not HATEOAS |
| API-08 OpenAPI/Swagger annotations | 0 | 0 | No @Operation/@ApiResponse annotations (auto-generated via springdoc) |
| **TOTAL** | | **10** | **/ 16 (63%)** |

---

## Code Quality (11 checks, max 22)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| CQ-01 Constructor injection | Field: 3, Constructor: 57 | 2 | Predominantly constructor injection |
| CQ-02 Lombok usage consistent | 0 @Data/@Getter/@Setter/@Builder | 0 | Lombok on classpath but records used instead for DTOs |
| CQ-03 No System.out/printStackTrace | 0 | 2 | Clean — no console output |
| CQ-04 Logging framework used | 204 @Slf4j/Logger references | 2 | Extensive SLF4J usage |
| CQ-05 Constants extracted | 333 static final/@Value | 2 | AppConstants.java centralizes all constants |
| CQ-06 DTOs separate from entities | Entities: 42, DTOs: 81 | 2 | Clean separation |
| CQ-07 Service layer exists | 61 service files | 2 | Full service layer |
| CQ-08 Repository layer exists | 23 repositories | 2 | Full repository layer |
| CQ-09 Doc comments on classes = 100% | 156 / 156 (100%) | 2 | **PASS** — All classes documented |
| CQ-10 Doc comments on public methods = 100% | 232 / 497 (46.7%) | 0 | **FAIL — BLOCKING** — 265 undocumented public methods |
| CQ-11 No TODO/FIXME/placeholder/stub | 2 found (UsbResetWatcherService Javadoc "stubbed") | 0 | **FAIL — BLOCKING** — stub pattern in documentation |
| **TOTAL** | | **0** | **/ 22 (0%) — BLOCKED by CQ-10 and CQ-11** |

**BLOCKING:** CQ-10 below 100% and CQ-11 has stub references → entire Code Quality category scores 0.

---

## Test Quality (12 checks, max 24)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| TST-01 Unit test files | 103 | 2 | Extensive unit test coverage |
| TST-02 Integration test files | 24 | 2 | Comprehensive integration tests |
| TST-03 Real database in ITs | 3 Testcontainers references | 2 | PostgreSQL Testcontainers used |
| TST-04 Source-to-test ratio | N/A | - | Not scored separately |
| TST-05a Unit test coverage = 100% | Not measured (requires `mvn test`) | ? | JaCoCo configured with 100% LINE+BRANCH enforcement |
| TST-05b Integration test coverage = 100% | Not measured (requires `mvn verify`) | ? | JaCoCo IT report configured |
| TST-05c Combined coverage = 100% | Not measured | ? | JaCoCo check goal enforces 100% |
| TST-06 Test config exists | 1 (application-test.yml) | 2 | Test profile configured |
| TST-07 Security tests | 141 references | 2 | @WithMockUser, Authorization Bearer |
| TST-08 Auth flow e2e | 189 references | 2 | Extensive auth integration tests |
| TST-09 DB state verification in ITs | 68 references | 2 | Repository verification in ITs |
| TST-10 Total @Test methods | 1009 unit + 136 integration = 1145 | 2 | Very high test count |
| **TOTAL** | | **16** | **/ 24 (67%) — Coverage not measured** |

**Note:** TST-05 checks require running `mvn test jacoco:report` and `mvn verify`. JaCoCo is configured to enforce 100% LINE+BRANCH coverage (excluding dto/model/Application). If build passes, coverage meets 100%. Build was not run during this audit.

---

## Infrastructure (6 checks, max 12)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| INF-01 Non-root Dockerfile | YES | 2 | User `myoffgridai` |
| INF-02 DB ports localhost only | No docker-compose.yml | 0 | No compose file to evaluate |
| INF-03 Env vars for prod secrets | 24 env var references | 2 | Full env var externalization |
| INF-04 Health check endpoint | 46 references | 2 | Actuator + custom health |
| INF-05 Structured logging | 6 references | 2 | LogstashEncoder JSON logging in prod |
| INF-06 CI/CD config | 0 | 0 | No CI/CD pipeline detected |
| **TOTAL** | | **8** | **/ 12 (67%)** |

---

## Security Vulnerabilities — Snyk (5 checks, max 10)

| Check | Result | Score | Notes |
|-------|--------|-------|-------|
| SNYK-01 Zero critical dependency vulns | PASS (0 critical) | 2 | No critical vulnerabilities |
| SNYK-02 Zero high dependency vulns | FAIL (1 high) | 0 | **BLOCKING** — tomcat-embed-core Incorrect Authorization |
| SNYK-03 Medium/low dependency vulns | 1 low | 1 | logback-core 1.5.22 |
| SNYK-04 Zero code (SAST) errors | SKIPPED | 0 | Snyk Code not enabled for org |
| SNYK-05 Zero code (SAST) warnings | SKIPPED | 0 | Snyk Code not enabled for org |
| **TOTAL** | | **0** | **/ 10 (0%) — BLOCKED by SNYK-02** |

**BLOCKING:** SNYK-02 has 1 HIGH dependency vulnerability → entire Snyk category scores 0.

---

## Scorecard Summary

| Category | Score | Max | % |
|----------|-------|-----|---|
| Security | 16 | 20 | 80% |
| Data Integrity | 10 | 16 | 63% |
| API Quality | 10 | 16 | 63% |
| Code Quality | 0 | 22 | 0% (**BLOCKED**) |
| Test Quality | 16 | 24 | 67% |
| Infrastructure | 8 | 12 | 67% |
| Snyk Vulnerabilities | 0 | 10 | 0% (**BLOCKED**) |
| **OVERALL** | **60** | **120** | **50%** |

**Grade: D (40-54%)**

---

## Blocking Issues

1. **CQ-10 — Public method documentation at 46.7% (needs 100%)**: 265 public methods lack Javadoc. Primarily in service and controller classes.

2. **CQ-11 — Stub pattern found**: `UsbResetWatcherService.java:20,45` contains "stubbed for MI-002" in Javadoc comments. While this is documentation (not code), it matches the stub scan pattern.

3. **SNYK-02 — HIGH vulnerability in tomcat-embed-core@10.1.50**: "Incorrect Authorization" — fix available by upgrading Spring Boot to pull tomcat 9.0.114+.

---

## Categories Below 60%

### Code Quality (0% — BLOCKED)
- **CQ-10** (0): Only 232/497 public methods have Javadoc → BLOCKING
- **CQ-11** (0): 2 stub references found → BLOCKING
- CQ-02 (0): No Lombok annotations (uses records instead — not necessarily a failure)

### Snyk Vulnerabilities (0% — BLOCKED)
- **SNYK-02** (0): 1 HIGH vulnerability in tomcat-embed-core → BLOCKING
- SNYK-04 (0): Snyk Code scan not available (org-level limitation, not a code issue)
- SNYK-05 (0): Same as SNYK-04

---

## Remediation Priority

1. **Upgrade Spring Boot** to pull patched tomcat-embed-core (fixes SNYK-02 HIGH)
2. **Add Javadoc to 265 undocumented public methods** (fixes CQ-10 BLOCKING)
3. **Remove or rephrase "stubbed" references** in UsbResetWatcherService Javadoc (fixes CQ-11)
4. **Add @Version to key entities** (Conversation, User, SystemConfig) for optimistic locking (DI-02)
5. **Configure SSL/TLS for production** (SEC-10)
6. **Add CI/CD pipeline** (INF-06)
