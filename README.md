# 🐅 WildWatch: Wildlife Sightings Intelligence Platform

WildWatch aggregates real wildlife observation data from [GBIF](https://www.gbif.org/) and [iNaturalist](https://www.inaturalist.org/), stores it in a geospatial database, and layers AI on top of it: a conversational assistant that answers questions grounded in real ingested data, and a vision-based tool that identifies species from an uploaded photo.

> Built end-to-end: external API integration → PostGIS-backed persistence → cached query API → AI features → a custom React/Next.js frontend → full Docker containerization → AWS deployment.

---

## Demo

*(Add a screenshot or short GIF of the dashboard here: the species catalog, trend chart, and chat panel in action.)*

The project is not kept permanently live to avoid ongoing hosting costs. To run it yourself, see [Running Locally](#running-locally) below. It's a single `docker compose up` once your API key is set.

---

## What It Does

- **Species catalog**: browse recorded species for a region, sorted by observation frequency, with pagination
- **Trend charts**: observation counts over time per species, rendered as an animated growing-vine chart
- **Sighting map**: geospatial scatter of recent observations (PostGIS-powered nearby queries)
- **Ask a naturalist**: conversational Q&A about regional wildlife, answered by Claude and grounded strictly in the app's own ingested dataset (not general knowledge)
- **Photo identification**: upload a wildlife photo; Claude's vision capability identifies the likely species and cross-checks it against the region's recorded data

---

## Tech Stack

**Backend**
- Java 21, Spring Boot 4
- PostgreSQL + PostGIS (geospatial queries: nearby-observation search, region filtering)
- Redis (caching layer)
- Flyway (versioned schema migrations)
- Resilience4j (retry + circuit breaker around external API calls)
- Spring AI + Anthropic (Claude): conversational Q&A and vision-based photo ID

**Frontend**
- Next.js (App Router) + TypeScript
- Recharts, custom SVG (animated trend chart, illustrated backdrop)
- React Markdown (rendering AI chat responses)

**Data Sources**
- [GBIF Occurrence API](https://www.gbif.org/developer/occurrence)
- [iNaturalist API](https://api.inaturalist.org/v1/docs/)

**Infrastructure**
- Docker + Docker Compose (full local & deployed stack)
- AWS EC2 (backend, Postgres, Redis)
- Vercel (frontend)

---

## Architecture

```
┌─────────────┐        ┌──────────────────┐        ┌─────────────────┐
│   Next.js    │──────▶│   Spring Boot     │──────▶│  GBIF API         │
│  (Vercel)    │        │   Backend (EC2)   │        │  iNaturalist API  │
└─────────────┘        │                   │        └─────────────────┘
                        │  ┌─────────────┐  │
                        │  │  IngestionService │
                        │  └──────┬──────┘  │
                        │         ▼         │
                        │  ┌─────────────┐  │       ┌──────────────┐
                        │  │  PostGIS DB  │◀─┼──────▶│  Redis Cache  │
                        │  └─────────────┘  │       └──────────────┘
                        │         ▲          │
                        │  ┌──────┴──────┐  │        ┌───────────┐
                        │  │  Query APIs  │  │───────▶│  Claude    │
                        │  └─────────────┘  │        │ (Spring AI)│
                        └──────────────────┘        └───────────┘
```

**Data flow:** External APIs → `IngestionService` (dedup + mapping) → PostGIS-backed `observations`/`species` tables → cached query services → REST API → frontend. The AI layer (Q&A + photo ID) reads from the same query services, so answers are grounded in the app's actual data rather than the model's general knowledge.

---

## Project Structure

```
wildwatch/
├── backend/          Spring Boot API (ingestion, persistence, queries, AI)
├── frontend/         Next.js dashboard
├── docker-compose.yml
└── README.md
```

---

## Running Locally

**Prerequisites:** Docker Desktop, an [Anthropic API key](https://console.anthropic.com/)

```bash
git clone https://github.com/vedant1104/wildwatch.git
cd wildwatch
```

Create a `.env` file in the project root:
```
ANTHROPIC_API_KEY=your-key-here
```

Start everything:
```bash
docker compose up --build
```

This starts four containers: PostgreSQL+PostGIS, Redis, the Spring Boot backend, and the Next.js frontend.

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8080`

**Note:** the database starts empty. Ingestion endpoints (guarded behind a `dev` profile) are used to pull real data from GBIF/iNaturalist for a given region. See `TestController` in the backend for the available endpoints.

---

## Notable Engineering Challenges

A few things worth calling out from actually building this, not just scaffolding it:

- **Spring Boot 4's modularized autoconfiguration**: several dependencies that "just worked" via a single Maven dependency in Spring Boot 3 tutorials (Flyway, caching, RestClient) silently did nothing in Spring Boot 4 until the matching dedicated starter was added. Diagnosed by reading Spring's Conditions Evaluation Report line by line rather than guessing.
- **A real data-integrity bug**: most GBIF-sourced observations had their `place_name` field storing the *dataset name* instead of an actual location, silently limiting region-based filtering since early in the project. Found by inspecting the actual stored data rather than trusting the schema, fixed with a dedicated `region` column and a corrected backfill migration (the first backfill attempt itself had a scoping bug, caught by cross-checking counts before trusting the fix).
- **Provider trade-off under a hard constraint**: originally planned to use iNaturalist's own Computer Vision API for photo ID, but its API access requires an account with 2+ months of history and active community contributions. Pivoted to Claude's vision capability instead, keeping the same "AI identifies, backend verifies against real data" design.
- **Redis serialization vs. polymorphic typing**: iterated through a few caching serializer configurations before landing on one that correctly round-trips typed DTOs without the fragility of Jackson's default polymorphic type embedding.
- **Build-time vs. runtime environment variables**: Next.js bakes `NEXT_PUBLIC_*` values into the compiled JS bundle at build time, not at container start. Required passing the backend URL as a Docker build argument, not just a runtime environment variable, to correctly point a containerized frontend at a remote backend.

---

## Possible Future Work

- Re-enable Redis caching on the two query methods currently running uncached
- Swap in iNaturalist's Computer Vision API once eligible, alongside the current Claude-vision approach
- Deeper historical ingestion across more regions for richer multi-year trend charts
- Kubernetes/EKS deployment as a follow-on infrastructure exercise
