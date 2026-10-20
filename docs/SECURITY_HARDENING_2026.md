# Security hardening review — 2026

This document records the focused security review performed against the RPM + ML branch and the controls added without changing the research functionality.

## High-priority findings addressed

| Area | Finding | Hardening applied |
|---|---|---|
| Credentials | Default JWT/admin/demo/ML secrets were available as fallbacks or embedded in the UI. | Removed production secret defaults; seed credentials are optional environment values; demo credentials removed from HTML. |
| Login | No application-level brute-force control. | Added bounded in-memory login throttling (default 5 failures / 15-minute window / 15-minute block). |
| Passwords | User creation accepted any non-empty password. | New or changed passwords require at least 15 characters, max 128, reject common fragments and repeated short patterns. New hashes use SHA-256 pre-hashing then BCrypt cost 12. Existing credentials are not rewritten by the schema migration. |
| Authorization | Global dashboard totals were reachable by any authenticated account; company dashboard accepted arbitrary IDs without a tenant check. | Global summary restricted to ADMINISTRADOR; per-company summary now validates company access. |
| Audit logs | Any authenticated user could call the audit-log controller and the HTTP API exposed create/delete operations. | Audit API is ADMINISTRADOR read-only. Application writes continue internally through `LogAuditoriaService`. |
| CORS | `@CrossOrigin("*")` existed on authentication/audit controllers and global CORS accepted broad headers/patterns. | Removed controller wildcards; exact origins, restricted methods/headers and no cross-origin cookies. |
| Evidence upload | File validation relied mainly on size/name and trusted the browser MIME type; files were read fully into heap memory. | Allow-list for PDF/PNG/JPG/TXT/CSV/DOCX/XLSX, MIME and magic-signature checks, Office container checks, UUID filenames and streaming SHA-256 persistence. |
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

## Before deploying this branch

The Render ML service and backend must contain the same strong secret value in `ML_API_KEY` and `RPM_ML_API_KEY`. If the currently configured ML key is shorter than 32 characters, rotate it before deployment. The backend must also have a strong `JWT_SECRET`. Keep `SEED_DEMO_DATA=false` for the public deployment.

Because the JWT expiration default is reduced to one hour, existing browser sessions will require a new login after deployment. The frontend now keeps tokens only for the active browser session.

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

1. Complete production backup/restore validation before applying the MFA schema migration. `DB_MIGRATIONS_ENABLED` defaults to false and must be enabled only for the controlled migration deploy.
2. Replace in-memory password-login throttling with a shared persistent control before scaling to multiple instances.
3. Add malware scanning/content-disarm for evidence uploads and immutable external audit storage.
4. Configure GitHub branch protection/rulesets and enforce successful security checks before merge.
5. Perform independent penetration testing and recurring SAST/DAST/dependency scanning; automated tests do not prove the absence of vulnerabilities.
6. Consider phishing-resistant authenticators (such as WebAuthn/passkeys) if the required assurance justifies the additional implementation and recovery workflow.

## Authentication behavior after this change

The regular login endpoint issues a JWT after valid credentials and does not start an MFA challenge. The web client therefore routes directly to the dashboard instead of waiting for a TOTP code. Users who select account recovery must complete the dedicated MFA challenge through `/api/auth/recovery/start` and `/api/auth/recovery/verify`. This is a product decision for the current prototype: it removes MFA from routine sign-in while preserving an additional factor for recovery. The trade-off should be reconsidered before a production rollout that requires phishing-resistant, multi-factor assurance on every session.
