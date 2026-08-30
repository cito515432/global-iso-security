# Security hardening review — 2026

This document records the focused security review performed against the RPM + ML branch and the controls added without changing the research functionality.

## High-priority findings addressed

| Area | Finding | Hardening applied |
|---|---|---|
| Credentials | Default JWT/admin/demo/ML secrets were available as fallbacks or embedded in the UI. | Removed production secret defaults; seed credentials are optional environment values; demo credentials removed from HTML. |
| Login | No application-level brute-force control. | Added bounded in-memory login throttling (default 5 failures / 15-minute window / 15-minute block). |
| Passwords | User creation accepted any non-empty password. | Central password policy: minimum 12 characters, maximum 128 and character-group checks. New BCrypt hashes use cost 12. |
| Authorization | Global dashboard totals were reachable by any authenticated account; company dashboard accepted arbitrary IDs without a tenant check. | Global summary restricted to ADMINISTRADOR; per-company summary now validates company access. |
| Audit logs | Any authenticated user could call the audit-log controller and the HTTP API exposed create/delete operations. | Audit API is ADMINISTRADOR read-only. Application writes continue internally through `LogAuditoriaService`. |
| CORS | `@CrossOrigin("*")` existed on authentication/audit controllers and global CORS accepted broad headers/patterns. | Removed controller wildcards; exact origins, restricted methods/headers and no cross-origin cookies. |
| Evidence upload | File validation relied mainly on size/name and trusted the browser MIME type; files were read fully into heap memory. | Allow-list for PDF/PNG/JPG/TXT/CSV/DOCX/XLSX, MIME and magic-signature checks, Office container checks, UUID filenames and streaming SHA-256 persistence. |
| ML API | Prediction endpoints became unauthenticated when `ML_API_KEY` was missing. | ML service now fails closed if the key is missing/short, uses constant-time comparison and protects metadata; backend requires the key too. |
| Browser session | JWT and role metadata persisted in `localStorage`. | Auth data moved to `sessionStorage` with one-time migration/cleanup of legacy values. |
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

## Remaining priorities

1. Add MFA for privileged roles.
2. Replace in-memory login throttling with Redis or another shared store if the backend scales to multiple instances.
3. Add malware scanning/content-disarm for evidence uploads.
4. Move audit records to immutable/WORM-capable external storage or add cryptographic chaining.
5. Configure GitHub branch protection/rulesets for `main` and the deployment branch.
6. Add independent penetration testing and recurring SAST/DAST/dependency scanning.
7. Reconcile `main` and `rpm-integral` so production code and portfolio documentation do not continue on diverged branches.
