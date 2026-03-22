# MyOffGridAI-Server — Quality Scorecard

**Generated:** 2026-03-22T00:11:41Z
**Branch:** main
**Commit:** 7366b30264c3dda20f68660a0ae7217e1925c70c Add shared knowledge vault — allow users to share documents with all household members

---

## Security (10 checks, max 20)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| SEC-01 | BCrypt/Argon2 password encoding | 22 refs (BCryptPasswordEncoder used throughout) | 2 |
| SEC-02 | JWT signature validation | 3 refs (signWith, parseClaimsJws, validateToken) | 2 |
| SEC-03 | SQL injection prevention (no string concat in queries) | 1 string concat query found | 1 |
| SEC-04 | CSRF protection | 0 — CSRF disabled (expected: stateless JWT API) | 2 |
| SEC-05 | Rate limiting configured | 2 refs (Bucket4j RateLimitFilter) | 2 |
| SEC-06 | Sensitive data logging prevented | 14 refs to password/secret/token near log statements | 1 |
| SEC-07 | Input validation on all endpoints | 38 @Valid/@Validated on controllers | 2 |
| SEC-08 | Authorization checks on admin endpoints | 40 @PreAuthorize/hasRole/hasAuthority refs | 2 |
| SEC-09 | Secrets externalized (not in code) | 0 hardcoded secrets in prod config (all via ${ENV_VAR}) | 2 |
| SEC-10 | HTTPS enforced in prod config | 0 — no server.ssl or require-ssl configuration | 0 |

**Security Score: 16 / 20 (80%)**

**Findings:**
- SEC-03: 1 potential string-concatenated query found — review for SQL injection risk
- SEC-06: 14 log references include password/secret/token terms — verify sensitive values are not logged
- SEC-10: No HTTPS/TLS configuration in application properties — relies on reverse proxy (nginx/ALB) for TLS termination

---

## Data Integrity (8 checks, max 16)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| DI-01 | All entities have audit fields | 36 / 43 entities have createdAt/updatedAt | 1 |
| DI-02 | Optimistic locking (@Version) | 0 — no @Version annotations | 0 |
| DI-03 | Cascade delete protection | 0 — no CascadeType.ALL/REMOVE | 0 |
| DI-04 | Unique constraints defined | 6 @Column(unique=true)/@UniqueConstraint | 2 |
| DI-05 | Foreign key constraints (JPA relationships) | 6 @ManyToOne/@OneToMany/@OneToOne/@ManyToMany | 2 |
| DI-06 | Nullable fields documented | 145 nullable=false/@NotNull annotations | 2 |
| DI-07 | Soft delete pattern | 0 — no soft delete implementation | 0 |
| DI-08 | Transaction boundaries defined | 81 @Transactional annotations | 2 |

**Data Integrity Score: 9 / 16 (56%)**

**Findings:**
- DI-01: 7 entities missing audit fields (createdAt/updatedAt)
- DI-02: No optimistic locking — concurrent write conflicts not prevented
- DI-03: No explicit cascade delete protection — orphan records possible
- DI-07: No soft delete pattern — deletes are permanent

---

## API Quality (8 checks, max 16)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| API-01 | Consistent error response format | 21 @ControllerAdvice/@ExceptionHandler | 2 |
| API-02 | Pagination on list endpoints | 115 Pageable/Page/PageRequest refs | 2 |
| API-03 | Validation on request bodies | 38 @Valid @RequestBody | 2 |
| API-04 | Proper HTTP status codes | 326 ResponseEntity/@ResponseStatus refs | 2 |
| API-05 | API versioning | 0 — no /api/v1 or versioned URL patterns | 0 |
| API-06 | Request/response logging | 2 LoggingFilter refs (RequestLoggingFilter + AuditAspect) | 2 |
| API-07 | HATEOAS/hypermedia (optional) | 22 Link refs (no Spring HATEOAS — likely false positives) | 1 |
| API-08 | OpenAPI/Swagger annotations | 0 — no @Operation/@ApiResponse/@Schema annotations | 0 |

**API Quality Score: 11 / 16 (69%)**

**Findings:**
- API-05: No API versioning in URL paths — all endpoints use /api/ without version prefix
- API-07: Link refs are likely generic Java Link references, not HATEOAS hypermedia
- API-08: No OpenAPI annotations — API spec is maintained manually, not via springdoc

---

## Code Quality (11 checks, max 22)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| CQ-01 | Constructor injection (not field injection) | Field: 5, Constructor: 66 (93% constructor) | 2 |
| CQ-02 | Lombok usage consistent | 0 — no Lombok (manual style, consistent) | 2 |
| CQ-03 | No System.out/printStackTrace | 0 found | 2 |
| CQ-04 | Logging framework used | 232 LoggerFactory/SLF4J refs | 2 |
| CQ-05 | Constants extracted | 379 static final/@Value refs | 2 |
| CQ-06 | DTOs separate from entities | Entities: 24 (in */model/), DTOs: 92 | 2 |
| CQ-07 | Service layer exists | 69 service files | 2 |
| CQ-08 | Repository layer exists | 24 repository files | 2 |
| CQ-09 | Doc comments on classes = 100% (BLOCKING) | **PASS (181 / 181 = 100%)** | 2 |
| CQ-10 | Doc comments on public methods = 100% (BLOCKING) | **FAIL (277 / 608 = 45.6%)** | 0 |
| CQ-11 | No TODO/FIXME/placeholder/stub (BLOCKING) | **PASS (0 found)** | 2 |

**Code Quality Score: 0 / 22 (0%) — BLOCKED by CQ-10**

> **BLOCKING:** CQ-10 doc coverage on public methods is 45.6% (277/608). Per scoring rules, the entire Code Quality category scores 0 when CQ-10 is below 100%. Without blocking, the raw score would be 20/22 (91%).

**Findings:**
- CQ-10: 331 public methods lack Javadoc (excludes DTOs, entities, and generated code)
- CQ-01: 5 remaining @Autowired field injections should be converted to constructor injection

---

## Test Quality (12 checks, max 24)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| TST-01 | Unit test files | 120 files | 2 |
| TST-02 | Integration test files | 25 files | 2 |
| TST-03 | Real database in ITs | 3 Testcontainers/PostgreSQLContainer refs | 2 |
| TST-04 | Source-to-test ratio | 145 tests / 96 source = 1.51x | 2 |
| TST-05a | Unit test coverage = 100% (BLOCKING) | **NOT VERIFIABLE** — 1 test failure prevents JaCoCo report | 0 |
| TST-05b | Integration test coverage = 100% (BLOCKING) | **NOT VERIFIABLE** — Docker/Testcontainers unavailable | 0 |
| TST-05c | Combined coverage = 100% (BLOCKING) | **NOT VERIFIABLE** — depends on TST-05a/b | 0 |
| TST-06 | Test config exists | 1 (application-test.yml) | 2 |
| TST-07 | Security tests | 148 @WithMockUser/Authorization refs | 2 |
| TST-08 | Auth flow e2e | 194 register/login/auth refs in ITs | 2 |
| TST-09 | DB state verification in ITs | 68 Repository/findBy/count refs in ITs | 2 |
| TST-10 | Total @Test methods | 1211 unit + 144 integration = 1355 total | 2 |

**Test Quality Score: 0 / 24 (0%) — BLOCKED by TST-05a/b/c**

> **BLOCKING:** Coverage reports could not be generated. 1 unit test failure (LlamaServerPropertiesTest.defaults_areSetCorrectly — expected context-size 32768 but got 4096) prevents JaCoCo report. Integration tests require Docker/Testcontainers (unavailable at audit time). Per scoring rules, the entire Test Quality category scores 0 when coverage is not verified at 100%. Without blocking, the raw score would be 16/24 (67%).

**Test Results Summary:**
- Unit tests: 1262/1263 PASS (99.92%), 1 FAIL
- Integration tests: 132 ERROR (Docker unavailable), 0 FAIL
- JaCoCo configured with 100% line + 100% branch enforcement (pom.xml)
- Failing test: `LlamaServerPropertiesTest.defaults_areSetCorrectly` — expected context-size=32768, actual=4096

---

## Infrastructure (6 checks, max 12)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| INF-01 | Non-root Dockerfile | YES (USER/adduser/addgroup present) | 2 |
| INF-02 | DB ports localhost only | No docker-compose.yml (standalone DB config) | 1 |
| INF-03 | Env vars for prod secrets | 24 ${ENV_VAR} refs in application-prod.yml | 2 |
| INF-04 | Health check endpoint | 73 health/actuator refs | 2 |
| INF-05 | Structured logging | 11 logback/LogstashEncoder refs (JSON in prod) | 2 |
| INF-06 | CI/CD config | 0 — no GitHub Actions/Jenkins/GitLab CI | 0 |

**Infrastructure Score: 9 / 12 (75%)**

**Findings:**
- INF-02: No docker-compose.yml — database is provisioned externally
- INF-06: No CI/CD pipeline — builds and deployments are manual

---

## Security Vulnerabilities — Snyk (5 checks, max 10)

| Check | Description | Result | Score |
|-------|-------------|--------|-------|
| SNYK-01 | Zero critical dependency vulnerabilities (BLOCKING) | **FAIL — 1 critical** | 0 |
| SNYK-02 | Zero high dependency vulnerabilities (BLOCKING) | **FAIL — 7 high** | 0 |
| SNYK-03 | Medium/low dependency vulnerabilities | 3 medium/low | 1 |
| SNYK-04 | Zero code (SAST) errors (BLOCKING) | **PASS — 0 errors** | 2 |
| SNYK-05 | Zero code (SAST) warnings | **PASS — 0 warnings** | 2 |

**Snyk Score: 0 / 10 (0%) — BLOCKED by SNYK-01, SNYK-02**

> **BLOCKING:** 1 critical + 7 high dependency vulnerabilities found. All are in Spring Boot 3.4.13 transitive dependencies. Primary remediation: upgrade to Spring Boot 3.5.12+. Per scoring rules, the entire Snyk category scores 0 when critical/high vulnerabilities exist. Without blocking, the raw score would be 5/10 (50%).

**Snyk Scan Details:**
- OSS: 1 Critical, 7 High, 0 Medium, 3 Low (11 total unique issues)
- SAST: 0 Errors, 0 Warnings, 0 Notes (clean)
- Primary fix: Upgrade `org.springframework.boot:spring-boot-starter-parent` from 3.4.13 to 3.5.12+

---

## Scorecard Summary

```
Category             | Score | Max |   %  | Status
─────────────────────|───────|─────|──────|──────────────────
Security             |    16 |  20 |  80% | B
Data Integrity       |     9 |  16 |  56% | C
API Quality          |    11 |  16 |  69% | C
Code Quality         |     0 |  22 |   0% | BLOCKED (CQ-10)
Test Quality         |     0 |  24 |   0% | BLOCKED (TST-05)
Infrastructure       |     9 |  12 |  75% | B
Snyk Vulnerabilities |     0 |  10 |   0% | BLOCKED (SNYK-01/02)
─────────────────────|───────|─────|──────|──────────────────
OVERALL              |    45 | 120 |  38% | F
```

**Grade: F (38%)**

> **3 categories blocked by mandatory checks.** Without blocking penalties, the adjusted scores would be:
> - Code Quality: 20/22 (91%) → A
> - Test Quality: 16/24 (67%) → C
> - Snyk: 5/10 (50%) → D
> - **Adjusted Overall: 86/120 (72%) → B**

---

## Blocking Issues (Must Fix)

1. **CQ-10 — Public Method Documentation (45.6%):** 331 public methods in service/controller/security/config/util classes lack Javadoc. Required: 100% coverage on all public methods excluding DTOs, entities, and generated code.

2. **TST-05 — Test Coverage Not Verifiable:** JaCoCo report cannot be generated due to 1 failing unit test (`LlamaServerPropertiesTest.defaults_areSetCorrectly`). Fix: update test expectation from 32768 to 4096 to match current `app.llama-server.context-size` config, OR update the config default to 32768.

3. **SNYK-01/02 — Critical/High Dependency Vulnerabilities:** 1 critical + 7 high vulnerabilities in Spring Boot 3.4.13 transitive dependencies. Fix: upgrade to Spring Boot 3.5.12+.

---

## Categories Below 60%

### Data Integrity (56%)
- **DI-02 (0):** No @Version optimistic locking on any entity
- **DI-03 (0):** No CascadeType.ALL/REMOVE protection
- **DI-07 (0):** No soft delete pattern — all deletes are permanent

### Code Quality (0% — BLOCKED)
- **CQ-10 (0):** 277/608 public methods documented (45.6%) — **BLOCKING ISSUE**

### Test Quality (0% — BLOCKED)
- **TST-05a/b/c (0):** Coverage not verifiable — **BLOCKING ISSUE**

### Snyk Vulnerabilities (0% — BLOCKED)
- **SNYK-01 (0):** 1 critical vulnerability — **BLOCKING ISSUE**
- **SNYK-02 (0):** 7 high vulnerabilities — **BLOCKING ISSUE**
