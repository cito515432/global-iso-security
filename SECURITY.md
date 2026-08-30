# Security Policy

Global ISO Security is an academic security-management prototype. Security findings should not be published with active credentials, tokens, database connection strings or exploit payloads that could affect the deployed demo.

## Reporting a vulnerability

Use a private channel with the repository owner and include:

- affected component and version/commit;
- reproduction steps using synthetic data;
- expected versus observed authorization boundary;
- impact and suggested remediation;
- no real personal data or third-party secrets.

## Production/deployment requirements

Before deploying a commit outside a local lab:

1. Configure `JWT_SECRET` with a random value of at least 32 characters.
2. Configure the same random value (at least 32 characters) in `ML_API_KEY` and `RPM_ML_API_KEY`.
3. Keep `SPRING_JPA_HIBERNATE_DDL_AUTO=none` and `SPRING_JPA_SHOW_SQL=false`.
4. Keep `SEED_DEMO_DATA=false` unless the environment is explicitly disposable.
5. Do not publish seed passwords, database credentials or API keys in HTML, JavaScript, README files, screenshots or commits.
6. Use TiDB/MySQL connections protected by TLS in production.
7. Store evidence on persistent/private storage and back it up.
8. Review the allowed frontend origin in `CORS_ALLOWED_ORIGINS`.
9. Rotate credentials if they were ever shown in a public screenshot or repository history.
10. Run the repository security workflow and dependency review before merging deployment changes.

## Current security controls

- Spring Security with stateless JWT authentication.
- BCrypt password hashing (cost 12 for newly encoded passwords).
- Backend RBAC with method security.
- Tenant/service authorization checks.
- Login throttling after repeated failures.
- Exact CORS origins and restricted headers.
- Append-only audit log HTTP API.
- Evidence path isolation, SHA-256 hashing, extension/MIME/signature validation and streamed uploads.
- Authenticated backend-to-ML inference API.
- Nginx security headers and CSP on the web frontend.
- Browser-session token storage rather than persistent local storage.

## Operational configuration

Production keeps `SEED_ENABLED=false`, `SEED_DEMO_DATA=false`, `SPRING_JPA_HIBERNATE_DDL_AUTO=none` and `SPRING_JPA_SHOW_SQL=false`. `DataInitializer` remains available only for controlled bootstrap, development, CI or recovery; it is not the normal production startup path.

The application exposes separate liveness (`/health`, `/api/health`) and readiness (`/readiness`, `/api/readiness`) checks. Readiness represents completion of the Spring application lifecycle, while liveness only indicates that the web process is responding.

Demo account identifiers and roles may be documented, but their password is managed out of band and must never be committed, logged or included in screenshots.

## Known limitations

This hardening does not make the prototype equivalent to a professionally certified product. Future production work should include MFA, centralized/distributed rate limiting, managed secrets, antivirus/content-disarm scanning for evidence files, immutable external audit storage, formal backup/restore testing, independent penetration testing, SAST/DAST gates and periodic key rotation.
