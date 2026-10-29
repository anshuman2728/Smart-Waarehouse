# Smart Warehouse — Inventory & Logistics Management Platform

[![CI](https://github.com/anshuman2728/Smart-Waarehouse/actions/workflows/ci.yml/badge.svg)](https://github.com/anshuman2728/Smart-Waarehouse/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)](https://react.dev)
[![Vite](https://img.shields.io/badge/Vite-6.0-646CFF?logo=vite&logoColor=white)](https://vitejs.dev)
[![Material UI](https://img.shields.io/badge/MUI-6.0-007FFF?logo=mui&logoColor=white)](https://mui.com)
[![TanStack Query](https://img.shields.io/badge/TanStack%20Query-5.59-FF4154?logo=reactquery&logoColor=white)](https://tanstack.com/query)

Smart Warehouse is a full-stack warehouse and inventory management platform that replaces manual (spreadsheet) stock tracking with a real-time system: multi-supplier management, stock movements, predictive reorder recommendations, low-stock alerts, and an analytics dashboard — secured with JWT and role-based access control.

## Tech Stack

### Backend

| Layer | Technology |
| ----- | ---------- |
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (RBAC: `ADMIN` / `MANAGER` / `EMPLOYEE`) |
| Persistence | Spring Data JPA, PostgreSQL |
| Real-time | WebSocket / STOMP |
| Mapping | MapStruct, Lombok |
| Validation | Spring Boot Validation |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Tests | JUnit 5, Mockito, Testcontainers, JaCoCo coverage |

### Frontend

- React 19, Vite 6, TypeScript 5.7 (strict mode)
- Tailwind CSS 3.4 + shadcn/ui-style components
- TanStack Query (React Query) v5 for server state management
- Material UI (MUI 6) + Headless UI primitives
- React Hook Form + Zod for validation
- STOMP (via `@stomp/stompjs`) + SockJS for real-time notifications
- Recharts for analytics, React Router v7 with protected/admin routes
- Testing: Vitest + React Testing Library + Playwright
- PDF Export: jsPDF + jsPDF-AutoTable

## Features

- **Authentication & Authorization** — register/login with JWT; roles `ADMIN`, `MANAGER`, `EMPLOYEE`
- **Predictive Reorder Recommendations Engine** — demand velocity forecasting from outbound movements ($ADD$), Reorder Point ($ROP$), Days of Cover ($DoC$), priority classification (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), explainable "Why Flagged?" formula inspector dialog, and CSV export
- **Product & Category management** — CRUD, stock status, low-stock detection
- **Stock Movement Tracking** — inbound, outbound, and inventory adjustments with before/after quantity validation
- **Suppliers** — multi-supplier management and purchase order tracking
- **Dashboard** — counts by status/category, low-stock list, recent movements, monthly stats
- **Advanced Reports** — interactive charts (bar, line, pie), supplier performance, PDF export with jsPDF
- **Notifications** — in-app notifications pushed in real time via WebSocket (including user-to-user)
- **Audit log** — records create/update/delete/login/register actions (ADMIN)
- **User management** — admin-only user CRUD with role assignment
- **Settings** — configurable application settings (ADMIN)
- **Type-safe API layer** — Axios interceptors with JWT injection, Zod schema validation
- **Opt-in Demo Data Seeding** — realistic sample business data ("Apex Central Logistics Hub") with idempotency and production guards

## Getting Started

### Prerequisites

- JDK 21
- Node.js 20+
- Docker & Docker Compose

### 1. Start the database

```bash
docker compose up -d
```

This starts PostgreSQL 17 on mapped host port `5433` (container port `5432`) with health checks and initialization scripts.

### 2. Run the backend

```bash
# In PowerShell:
$env:SEED_DEMO_DATA="true"; .\mvnw.cmd spring-boot:run

# In Bash / Linux / macOS:
SEED_DEMO_DATA=true ./mvnw spring-boot:run
```

The API starts on `http://localhost:8081` (Swagger UI: `http://localhost:8081/swagger-ui.html`).

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:3000` (Vite) and proxies `/api` and `/ws` to the backend on `http://localhost:8081`.

### Full Stack with Docker Compose

```bash
# Build and start all services
docker compose up -d --build

# View logs
docker compose logs -f

# Stop services
docker compose down

# Stop and remove volumes (database data)
docker compose down -v
```

Services:
- **PostgreSQL**: `localhost:5433`
- **Backend API**: `localhost:8081`
- **Frontend**: `localhost:3000`

Environment variables (create `.env` file):
```env
JWT_SECRET=your-super-secret-jwt-key-min-32-chars
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=SecurePassword123
```

### Development Commands (Frontend)

```bash
cd frontend

# Development
npm run dev           # Start Vite dev server
npm run build         # Type check + production build
npm run preview       # Preview production build

# Testing
npm run test          # Watch mode
npm run test:run      # Single run with coverage
npm run test:ui       # Visual test UI

# Code Quality
npm run lint          # ESLint
npm run format        # Prettier
```

On first startup, `DataInitializer` seeds the `ADMIN`, `MANAGER` and `EMPLOYEE` roles.

### Demo Data Seeding (Local Development)

To seed a realistic sample business with categories, suppliers, varied stock profiles (healthy, near-low, low, zero stock), stock movements, notifications, and demo users, enable `SEED_DEMO_DATA`:

```bash
# In PowerShell:
$env:SEED_DEMO_DATA="true"; ./mvnw spring-boot:run

# In Bash / Linux / macOS:
SEED_DEMO_DATA=true ./mvnw spring-boot:run
```

> **Safety Notice:** Demo data seeding is idempotent (restarting does not duplicate records) and is strictly blocked under the `prod` profile (`@Profile("!prod")`). Existing database volumes and user data are never deleted or overwritten.

#### Demo Credentials

| Role | Email | Password | Access |
| ---- | ----- | -------- | ------ |
| **Admin** | `admin@smartwarehouse.com` | `Admin@123` | Full administrative control, users, settings, and audit logs |
| **Manager** | `manager@smartwarehouse.com` | `Manager@123` | Inventory, stock movements, reorder recommendations, and reports |
| **Employee** | `employee@smartwarehouse.com` | `Employee@123` | Day-to-day stock movement recording, alerts, and notifications |

#### Seeded Sample Business ("Apex Central Logistics Hub")
- **Categories:** Sensors & Vision Systems, Microcontrollers & SBCs, Motors & Motion Control, Industrial Cabling.
- **Suppliers:** NovaTech Electronics, Apex Motion Systems, Global Wire & Cable Corp.
- **Products & Stock Profiles:**
  - `SBC-RPI4-8GB` (Raspberry Pi 4 8GB) — Stock: 120 (Healthy)
  - `CBL-CAT6-50M` (Cat6 50m Cable) — Stock: 85 (Healthy)
  - `PWR-DIN-24V10A` (24V 10A PSU) — Stock: 18 (Near-Low / Threshold)
  - `MTR-NEMA23-28` (NEMA 23 Stepper Motor) — Stock: 6 (Low Stock Alert)
  - `CAM-IND-5MP` (5MP Vision Camera) — Stock: 3 (Low Stock Alert)
  - `SNS-US-IP67` (Ultrasonic Proximity Sensor) — Stock: 0 (Out of Stock Alert)
  - `MCU-STM32-F4` (STM32F407 Board) — Stock: 14 (High Velocity Reorder Breach)
- **Stock Movements & Audit Logs:** All initial product stocks are established via `MovementService.inbound()` and `MovementService.outbound()`, ensuring full mathematical before/after balance and audit log recording.
- **Notifications:** Sample operational messages between Admin, Manager, and Employee.

## API Overview

| Module | Endpoint | Access |
| ------ | -------- | ------ |
| Auth | `/api/auth/register`, `/api/auth/login` | Public |
| Reorder Hub | `/api/reorder/recommendations`, `/api/reorder/export` | Authenticated |
| Products | `/api/products/**` | Authenticated |
| Categories | `/api/categories/**` | Authenticated |
| Movements | `/api/movements/**` | Authenticated |
| Suppliers | `/api/suppliers/**` | Authenticated |
| Dashboard | `/api/dashboard/**` | Authenticated |
| Reports | `/api/reports/**` (JSON, PDF export) | Authenticated |
| Notifications | `/api/notifications/**` | Authenticated |
| Settings | `/api/settings/**` | ADMIN |
| Users | `/api/users/**` | ADMIN |
| Audit logs | `/api/audit-logs/**` | ADMIN |

## Reorder Recommendation Endpoints

| Endpoint | Method | Description | Parameters |
| -------- | ------ | ----------- | ---------- |
| `/api/reorder/recommendations` | GET | Predictive replenishment calculations ($ADD$, $ROP$, $DoC$, priority, and rule breakdown) | `days` (default 30), `leadTimeDays` (default 7) |
| `/api/reorder/export` | GET | Download reorder recommendations as a CSV spreadsheet | `days` (default 30), `leadTimeDays` (default 7) |

## Report Endpoints

| Endpoint | Method | Description |
| -------- | ------ | ----------- |
| `/api/reports/inventory` | GET | Full inventory analytics (counts, value, movements, top products, supplier performance) |
| `/api/reports/inventory/pdf` | GET | PDF export of inventory report |
| `/api/reports/movements` | GET | Stock movement analytics with date filtering |
| `/api/reports/products` | GET | Product performance report |

All report endpoints support optional `startDate` and `endDate` query parameters (ISO 8601 format).

## Project Structure

```
com.saadaoui.smartwarehouse
├── auth          # JWT, security config, authentication, user service
├── category      # product categories
├── config        # OpenAPI documentation & opt-in demo data seeder
├── dashboard     # summary statistics
├── movement      # stock movements (in / out / adjustment)
├── notification  # notifications + real-time WebSocket push
├── product       # products and stock status
├── reorder       # predictive reorder recommendations engine & CSV export
├── report        # CSV + PDF reports with analytics
├── settings      # application settings
├── supplier      # suppliers
├── user          # user management (ADMIN)
├── audit         # audit logging
├── websocket     # STOMP configuration
├── entity        # JPA entities
└── exception     # centralized error handling
```

### Frontend Structure

```
frontend/src
├── api           # API client, endpoints, types
├── components    # React components
│   ├── ui        # shadcn/ui-style reusable components
│   ├── layout    # Layout, Sidebar, Navbar
│   └── common    # Loading, ErrorBoundary, etc.
├── context       # React Context providers (Auth, Theme)
├── features      # Feature-specific components
├── hooks         # Custom React hooks
├── pages         # Page components (lazy-loaded)
├── routes        # React Router configuration
├── types         # TypeScript types
├── utils         # Utility functions (cn, download, etc.)
├── test          # Vitest setup and utilities
├── App.tsx       # Root component
├── main.tsx      # Entry point with providers
└── index.css     # Tailwind CSS imports
```

## Testing

### Backend

The backend follows a testing pyramid:

- **Unit tests** — services with mocked repositories (JUnit 5 + Mockito)
- **Integration tests** — real PostgreSQL via **Testcontainers**, exercising JWT-secured API flows

Run the full suite with coverage:

```bash
./mvnw verify
```

Coverage is enforced with **JaCoCo** (≥70% instruction coverage gate) and the report is generated at `target/site/jacoco/index.html`.

### Frontend

- **Unit tests** — Vitest + React Testing Library
- **E2E tests** — Playwright (to be configured)
- **Type checking** — TypeScript strict mode (`tsc --noEmit`)
- **Linting** — ESLint with React hooks rules

```bash
cd frontend
npm run test:run    # Run tests with coverage
npm run lint        # Lint check
```

## CI/CD

`.github/workflows/ci.yml` runs on every push/PR to `main`:

### Backend
- `mvnw verify` — tests + Testcontainers + JaCoCo coverage gate (≥70%)
- Codecov upload for coverage tracking

### Frontend
- ESLint + TypeScript strict type checking
- Vitest unit tests with coverage
- Production build (`npm run build`)

### Security
- Trivy filesystem vulnerability scan
- SARIF upload to GitHub Security tab

### Docker
- Multi-stage builds for backend (Maven → JRE Alpine) and frontend (Node → Nginx Alpine)
- Non-root user execution
- Build & push to GitHub Container Registry (GHCR) with BuildKit caching
- Separate images for backend and frontend

### Deployment
- Staging deployment on merge to `main` (manual approval via environment)

## Recent Improvements (v0.2.0)

### Frontend Enhancements
- **Component Library**: Added shadcn/ui-style components with Tailwind CSS (Button, Card, Input, Table, Badge, Skeleton)
- **State Management**: Integrated TanStack Query (React Query) v5 for server state with caching, retries, and deduplication
- **TypeScript Strictness**: Enabled strict mode with `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noUnusedLocals/Parameters`
- **Path Aliases**: Configured `@/` imports for cleaner imports
- **Testing Setup**: Vitest + React Testing Library + jsdom with coverage reporting
- **Toast Notifications**: react-hot-toast for user feedback
- **Utility Functions**: `cn()` helper (clsx + tailwind-merge) for conditional class names

### Docker & Infrastructure
- **Multi-stage Builds**: Optimized backend (Maven → JRE Alpine) and frontend (Node → Nginx Alpine) images
- **Security Hardening**: Non-root users, minimal base images, health checks
- **Resource Limits**: Memory limits and reservations for all containers
- **Database Initialization**: PostgreSQL extensions (uuid-ossp, pg_trgm, btree_gin) via init scripts
- **Network Isolation**: Dedicated Docker network

### CI/CD Pipeline
- **Parallel Jobs**: Backend, Frontend, Security scans run in parallel
- **Security Scanning**: Trivy vulnerability scanner with SARIF upload to GitHub Security
- **Separate Docker Images**: Backend and frontend images pushed independently to GHCR
- **BuildKit Caching**: GitHub Actions cache for faster builds
- **Staging Deployment**: Environment-based deployment with manual approval

### New Feature: Advanced Reports
- **Interactive Charts**: Bar charts (products by category, inventory value), Line chart (stock movements), Pie chart (top products)
- **Supplier Performance Table**: Sortable table with product counts and total values
- **PDF Export**: One-click PDF generation with jsPDF + jsPDF-AutoTable
- **Date Range Filtering**: Dynamic date range picker for report period selection
- **Real-time Data**: Powered by TanStack Query with automatic refetching

## License

Smart Warehouse is open-source under the MIT License.
