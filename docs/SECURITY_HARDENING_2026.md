# Security hardening review — 2026

This document records the focused security review performed against the RPM + ML branch and the controls added without changing the research functionality.

## High-priority findings addressed

| Area | Finding | Hardening applied |
|---|---|---|
| Credentials | Default JWT/admin/demo/ML secrets were available as fallbacks or embedded in the UI. | Removed production secret defaults; seed credentials are optional environment values; demo credentials removed from HTML. |
| Login | Brute-force state was local to one process. | Added shared database-backed throttling (5 failures / 15-minute window / 15-minute block), keyed by a digest; no artificial delay is added to requests. Requires migration V2 before enabling the new backend. |
| Passwords | User creation accepted any non-empty password. | New or changed passwords require at least 8 characters, including uppercase, lowercase, number and special character; maximum 128, reject common fragments and repeated short patterns. New hashes use SHA-256 pre-hashing then BCrypt cost 12. Existing credentials are not rewritten. |
| Authorization | Global dashboard totals were reachable by any authenticated account; company dashboard accepted arbitrary IDs without a tenant check. | Global summary restricted to ADMINISTRADOR; per-company summary now validates company access. |
| Audit logs | Any authenticated user could call the audit-log controller and the HTTP API exposed create/delete operations. | Audit API is ADMINISTRADOR read-only and returns the latest 200 events. Login and selected account/role changes now add events; coverage is not yet exhaustive or externally immutable. |
| CORS | `@CrossOrigin("*")` existed on authentication/audit controllers and global CORS accepted broad headers/patterns. | Removed controller wildcards; exact origins, restricted methods/headers and no cross-origin cookies. |
| Evidence upload | File validation alone could not detect malware. | Existing allow-list, MIME/signature, Office-container, size and storage-path checks remain; an optional ClamAV `INSTREAM` scan now runs before the evidence row is saved. `AV_REQUIRED=true` rejects uploads when the private scanner is unavailable. This does not guarantee detection of every malicious file. |
| Password recovery | The former MFA recovery flow did not change a forgotten password. | Added a separate email reset flow with generic responses, throttling, random single-use 15-minute tokens stored only as hashes, password policy checks and invalidation of existing JWT sessions. It requires V2 and SMTP configuration; it is disabled until configured. Existing MFA recovery remains separate. |
| Custom roles | Arbitrary role labels did not map safely to the backend's fixed authorization checks. | The admin UI can create a role and assign one of four fixed profiles. Custom roles cannot inherit ADMINISTRADOR; legacy roles without a valid profile fall back to USUARIO_EMPRESA. |
| ML API | Prediction endpoints became unauthenticated when `ML_API_KEY` was missing. | ML service now fails closed if the key is missing/short, uses constant-time comparison and protects metadata; backend requires the key too. |
| Browser session | JWT and role metadata persisted in `localStorage`. | Auth data moved to `sessionStorage` with one-time migration/cleanup of legacy values. |
| MFA | El segundo factor se exigía en cada inicio de sesión, incluso cuando no se estaba recuperando una cuenta. | El inicio de sesión cotidiano usa correo + contraseña; MFA se reserva al flujo explícito de recuperación, con retos de un solo uso, prevención de repetición, códigos de recuperación con hash, semillas cifradas con AES-256-GCM y revocación de sesiones al restablecer MFA. |
| Password storage | BCrypt truncates inputs beyond 72 UTF-8 bytes. | New passwords are SHA-256 pre-hashed then BCrypt-encoded; historical BCrypt hashes remain verifiable for inputs within the old limit. |
| Web headers | Nginx had no explicit CSP/security headers. | Added CSP, clickjacking protection, MIME sniffing protection, referrer/permissions policies and HSTS on Render. |
| Local deployment | Docker Compose shipped weak fallback credentials and published MySQL on all interfaces. | Required secrets in `.env`, disabled demo data by default, bound local ports to loopback and aligned upload limits. |
| Repository hygiene | Generated Python bytecode and accidental empty root files were tracked. | Removed artifacts and expanded `.gitignore`. |

## Intentionally unchanged

- CSRF remains disabled because authentication is stateless and the browser sends JWTs in the `Authorization` header rather than an authentication cookie.
- `/health` remains public so Render can perform health checks.
- Public certificate verification remains public by design and returns a limited data set; verification codes have high entropy.
- RPM deterministic behavior, ML model weights, human validation and RPM memory semantics were not altered by this hardening pass.

## Before deploying the security-controls branch

The Render ML service and backend must contain the same strong secret value in `ML_API_KEY` and `RPM_ML_API_KEY`. If the currently configured ML key is shorter than 32 characters, rotate it before deployment. The backend must also have a strong `JWT_SECRET`. Keep `SEED_DEMO_DATA=false` for the public deployment.

Because the JWT expiration default is reduced to one hour, existing browser sessions will require a new login after deployment. The frontend now keeps tokens only for the active browser session.

Before deploying this branch, apply the controlled schema migration V2 after checking a restorable TiDB backup. The new login limiter reads `login_attempt_buckets`; until V2 exists, login requests on this code will fail instead of silently disabling throttling. The password reset endpoint also needs `password_reset_tokens`.

Set `PASSWORD_RESET_ENABLED=true` only after `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `PASSWORD_RESET_FROM`, and the HTTPS `PASSWORD_RESET_FRONTEND_URL` are configured. Reset tokens are placed in the URL fragment so browsers do not send them to the web server in normal HTTP requests or Referer headers.

Keep `AV_REQUIRED=true` in the public deployment and configure `AV_CLAMD_HOST` to a private scanner endpoint on port 3310. Do not expose ClamAV's TCP port publicly: the ClamAV documentation warns that TCP connections lack transport encryption and protocol-level protections. The official ClamAV container documentation recommends roughly 3 GiB minimum and 4 GiB preferred RAM, so the current small Render service should not host the scanner itself; use a properly sized private service. See [ClamAV Docker guidance](https://docs.clamav.net/manual/Installing/Docker.html).

## State and verification of this patch — 2026-10-20

- Changes are in draft PR #43 on `security/mobile-mfa-hardening-2026`; they have not been merged.
- Render already has the backend service, but its live deploy remains on commit `4cf9580e792bb2750ff2788d154b184e3910fd2a`; automatic deploy is disabled. TiDB production has not been changed.
- The MFA schema migration only creates `usuario_mfa`, `mfa_login_challenges` and `mfa_recovery_codes`. It is disabled by default. `database/tidb-migrator` is a separate one-shot Flyway runner that packages the backend's exact migration resource, defaults to read-only `info`, requires verified-backup and target confirmations for writes, enforces TiDB Cloud hostname/TLS checks, and stops on pre-existing/partial MFA tables. It must run only after a verified, restorable TiDB backup.
- The updated web login must deploy with the backend because both use the same login API contract. The mobile app fails closed against the current backend until the MFA backend is deployed.
- GitHub Actions run 39461239790 passed all six jobs on commit `45f56cfbd4ccf583a64e4683a53d8bf99e5acc07`: backend tests, packaged-JAR liveness/readiness, MySQL integration for service creation and SoA repair, controlled migrator packaging, ML API-key smoke test plus `pip-audit`, and OSV dependency scanning of the backend and migrator Maven manifests. The scan found no known vulnerable dependencies in those manifests at the time of the run; this is an advisory-database snapshot, not proof that software is free of vulnerabilities.
- Spring Boot was upgraded to 4.1.1 so the application uses the OSS Spring Framework 7.0.9 baseline; the code retains the Spring Boot Jackson 2 compatibility module while its DTOs/services still import Jackson 2. Java dependency overrides also raise Tomcat and Jackson to versions cleared by the scan. The Python dependency set remains checked with `pip-audit`.
- The CI integration test uses isolated MySQL 8, not the production TiDB instance. The migrator has not connected to TiDB. Android Gradle tests were not run because this workspace has no Gradle executable or wrapper. No production penetration test or DAST was performed.
- The separate migrator has not been built or run against TiDB; no production connection, schema change, Render environment change, or deployment has occurred.

## Remaining priorities

1. Validate the restorable TiDB backup and apply V1/V2 through the guarded migrator before deploying backend code that depends on them.
2. Configure and test SMTP and a private, adequately provisioned ClamAV endpoint. The local Docker example leaves AV scanning off for development unless a scanner is configured; the Render Blueprint requires scanning.
3. Configure GitHub branch protection/rulesets and enforce successful security checks before merge.
4. Expand audit coverage and consider an append-only external destination with retention controls.
5. Perform independent penetration testing and recurring SAST/DAST/dependency scanning; automated tests do not prove the absence of vulnerabilities.
6. Consider phishing-resistant authenticators (such as WebAuthn/passkeys) if the required assurance justifies the additional implementation and recovery workflow.

## Authentication behavior after this change

The regular login endpoint issues a JWT after valid credentials and does not start an MFA challenge. The web client therefore routes directly to the dashboard instead of waiting for a TOTP code. Users who select account recovery must complete the dedicated MFA challenge through `/api/auth/recovery/start` and `/api/auth/recovery/verify`. This is a product decision for the current prototype: it removes MFA from routine sign-in while preserving an additional factor for recovery. The trade-off should be reconsidered before a production rollout that requires phishing-resistant, multi-factor assurance on every session.

## Additional controls under review — 2026-10-21

The changes in the current branch are code and configuration changes only. They do not apply database migrations, modify Render's live environment, or deploy the application. Migration V2 is additive and creates only the rate-limit and password-reset tables. The normal password login contract and role-based dashboard routing remain intact; custom roles use their assigned fixed profile for server-side authorization. Local development keeps the scanner optional by default so existing upload demos can run, while the public Render Blueprint requires scanning and blocks uploads until the private scanner is reachable. Production activation still requires the migration and service configuration above.

## Consolidated implementation and release status — 2026-10-21 (current)

This section supersedes earlier draft/branch status statements in this document. The implementation is proposed in draft PR [#47](https://github.com/cito515432/global-iso-security/pull/47), branch `codex/security-controls-2026-10`, head `1bd75905c64b5a0e006cd393862add3dcad00eee`. It preserves the existing RPM deterministic calculation, ML second-estimate contract, user/role dashboard routes, and ordinary email-and-password sign-in. The security changes are additive or narrowly scoped; they do not claim complete protection against every attack.

### Implemented and tested in the branch

- Persistent, shared login throttling: five failed attempts within a 15-minute window produce a 15-minute block. It adds no per-request delay. Its table must exist before the new backend is deployed.
- Password policy for new or changed passwords: at least eight characters with uppercase, lowercase, digit, and special character; the configured maximum is 128 characters. New hashes use SHA-256 pre-hashing followed by BCrypt cost 12; historical hashes are not mass-rewritten.
- Account recovery: a separate, generic-response email reset flow uses a random single-use token stored as a hash, expires after 15 minutes, validates the password policy, and invalidates prior JWT sessions. The feature remains disabled until SMTP is configured and `PASSWORD_RESET_ENABLED=true` is deliberately set.
- MFA behavior: the regular login path does not require MFA. The existing dedicated account-recovery challenge remains a separate flow; this is not equivalent to mandatory MFA on every session.
- Role creation: custom roles receive one of the fixed, least-privilege operational profiles; they cannot be assigned the administrator profile by this feature. Authorization remains enforced on the backend.
- Evidence uploads: existing extension, MIME/signature, Office-container, size, and storage checks are retained, with a ClamAV `INSTREAM` scan before saving the evidence record. With `AV_REQUIRED=true`, an absent/unreachable scanner rejects uploads (fail closed); it does not prove detection of all malicious content.
- Authorization and tenant boundaries: checks were added/strengthened for company-scoped and role-scoped operations. Audit events cover selected authentication, account, and role changes; the log is not represented as exhaustive or tamper-proof.
- Browser and link safety: session tokens use session-scoped storage with legacy cleanup; training links are limited to HTTPS or safe same-app paths and rendered without unsafe protocols.
- Production configuration: Hibernate schema mutation and SQL echo remain off, seeds remain off, and the TiDB migrator is separate from normal backend startup. The ML API key and JWT secret have no insecure production fallback.

### Verification evidence

GitHub Actions run [39643412884](https://github.com/cito515432/global-iso-security/actions/runs/39643412884), run number 71, completed successfully for commit `1bd75905c64b5a0e006cd393862add3dcad00eee`. All six jobs passed:

| CI job | Scope verified |
|---|---|
| `backend-build` | Backend compilation and automated tests |
| `backend-packaged-smoke` | Packaged JAR startup and liveness/readiness endpoints |
| `backend-mysql-integration` | Service creation and SoA creation/repair against isolated MySQL 8 |
| `tidb-migrator-build` | Packaging of the guarded TiDB migrator |
| `ml-validation` | Python compilation, health/API-key smoke tests, and `pip-audit` |
| `dependency-sca / osv-scan` | Dependency scan for the configured repository manifests |

CI is evidence for the tested code paths, not a production penetration test, DAST assessment, or guarantee that no vulnerabilities exist. The integration database is isolated MySQL 8; CI does not connect to production TiDB.

### Production status and mandatory rollout gates

As of this record, PR #47 is still a draft and has not been merged. The production TiDB schema, Render environment variables, and running services have not been changed by this work. No production login, upload, email, or tenant-isolation test has been claimed. The following sequence is required before deployment:

1. Confirm the intended Render workspace and identify the exact backend, frontend, and ML services; verify their current live commits and controlled/manual deployment settings.
2. Confirm a TiDB backup exists and is restorable. Run the migrator's read-only `info` and `validate` modes, review the target database and migration history, then authorize and run V2 with the explicit confirmation variables described in `database/tidb-migrator/README.md`. Do not deploy the new backend first: login throttling and password reset depend on the V2 tables. Do not run the migration concurrently with backend startup or another migrator.
3. Verify required secrets/configuration in the service's secret manager without printing their values: strong `JWT_SECRET`; matching, sufficiently strong `ML_API_KEY` and `RPM_ML_API_KEY`; and a valid `MFA_ENCRYPTION_KEY`. Keep `SEED_ENABLED=false`, `SEED_DEMO_DATA=false`, `SPRING_JPA_HIBERNATE_DDL_AUTO=none`, `SPRING_JPA_SHOW_SQL=false`, and `DB_MIGRATIONS_ENABLED=false` in the normal backend service.
4. Configure and test the SMTP sender and HTTPS reset-page URL before enabling `PASSWORD_RESET_ENABLED=true`. Until then, the recovery-by-email feature is intentionally inactive; do not advertise it as operational.
5. Provide a private, reachable, adequately provisioned ClamAV endpoint at `AV_CLAMD_HOST`/`AV_CLAMD_PORT` in the same permitted network. Keep `AV_REQUIRED=true`; never expose the ClamAV TCP port publicly. Until this is available, uploads are intentionally rejected and the complete upload demonstration is not ready.
6. Make the PR ready for review and merge only after its required checks remain green and the release gates above are satisfied. Deploy the intended commit to the identified services in a controlled order, respecting the existing manual-deploy policy.
7. Monitor each deployment to a terminal `live` state. Check `/health` and `/readiness`, then perform authorized smoke tests for ordinary sign-in, role-based pages, same-tenant versus cross-tenant access, login lockout, reset email/token lifecycle, custom-role restrictions, audit events, clean-file upload, and scanner-unavailable fail-closed behavior. Check logs for new exceptions without recording passwords, tokens, reset URLs, or uploaded content.
8. Record deployed commit/deploy IDs, migration outcome, checks, and any rollback decision here after the work is actually completed. A code rollback does not reverse an applied database migration; verify backward compatibility before rolling back application code.

The exact Render workspace is not yet confirmed for this rollout. Therefore no Render service has been inspected or mutated as part of this deployment authorization, and no deploy should be described as complete until the workspace and release gates above are confirmed.
