# Global ISO Security

Role-based web application for ISO/IEC 27001 Statement of Applicability (SoA), risks, evidence, audits, training and an explainable RPM decision model.

[![Java](https://img.shields.io/badge/Java-20-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot) [![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/) [![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

> A multi-company security-management prototype connecting controls, risks, evidence and human review in one workflow.

## What it demonstrates

- **93** ISO/IEC 27001:2022 reference controls and service-level SoA.
- Risk registration, risk–control relationships and follow-up.
- Evidence upload/download, SHA-256 hashing, validity and verification.
- Audit findings, recurrence and closure.
- Deterministic, explainable RPM with priority, response, validation and memory.
- Training, assessments, verifiable certificates and executive reporting.
- JWT authentication, BCrypt hashing, role authorization and company isolation.

## Architecture

~~~mermaid
flowchart TB
 A[Role-based HTML CSS JS frontend] --> B[Spring Boot REST API]
 B --> C[SoA risks evidence audits training RPM]
 C --> D[MySQL]
 C --> E[Docker evidence volume]
~~~

## Roles

| Role | Responsibility |
|---|---|
| ADMINISTRADOR | Companies, users, services, catalog, roles and reports |
| IMPLEMENTADOR | Context, SoA, risks, evidence and RPM analysis |
| AUDITOR | Evidence validation, findings, signatures and RPM decisions |
| CAPACITADOR | Training, assessments, certificates and formative actions |
| USUARIO_EMPRESA | Executive portal, progress, risks, training and reports |

See the complete [permission matrix](docs/MATRIZ_ROLES_PERMISOS.md).

## Stack and evidence map

Java 20 · Spring Boot 3.2 · Spring Security · JWT · JPA/Hibernate · MySQL · HTML5 · CSS3 · JavaScript · Docker Compose · Nginx.

| Area | Current scope |
|---|---|
| Compliance | 93-control catalog, SoA and applicability rationale |
| Risk and audit | Risks, findings, recurrence and closure |
| Evidence | Persistent files, SHA-256 and verification |
| RPM | Detection, response, coordination, evaluation and memory |
| Reporting | PDF and Excel outputs |

## Run locally

~~~bash
cp .env.example .env
# Set local passwords and a strong JWT_SECRET; never commit .env.
docker compose up --build
~~~

Open http://localhost:8080/pages/login.html. Demo seed data is controlled by environment variables; never publish passwords in documentation or commit .env.

Useful documentation: [README_DOCKER.md](README_DOCKER.md), [README_RENDER.md](README_RENDER.md), [docs/ARQUITECTURA_RPM.md](docs/ARQUITECTURA_RPM.md), [docs/GUIA_PRUEBAS.md](docs/GUIA_PRUEBAS.md), [ENTREGA_RPM.md](ENTREGA_RPM.md), [IMPLEMENTACION_RPM_INTEGRAL.md](IMPLEMENTACION_RPM_INTEGRAL.md).

## Limitations

This is an academic and demonstration system. Production use would require threat modeling, deployment hardening, managed secrets, security testing, backup/recovery and formal compliance review.

## Related case study

[Read the technical case study](https://andres-obando-portfolio-static.onrender.com/case-studies/global-iso-security/)

## Author

**Andrés Obando** · [GitHub](https://github.com/cito515432) · [LinkedIn](https://www.linkedin.com/in/andres-obando-08095b203) · [Portfolio](https://andres-obando-portfolio-static.onrender.com/)
