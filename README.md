# HackConnect — Backend

A production-ready Spring Boot REST API for HackConnect: the one-stop platform where college students discover hackathons, competitions, and internships, and follow curated learning roadmaps with per-step progress tracking.

---

## Tech Stack

| Layer       | Technology                                    |
|-------------|-----------------------------------------------|
| Runtime     | Java 17 · Spring Boot 3.2                    |
| Web         | Spring MVC (REST)                             |
| Security    | Spring Security 6 · JWT (jjwt 0.11)          |
| Persistence | Spring Data JPA · Hibernate · H2 (dev) · PostgreSQL (prod) |
| Build       | Maven 3.8+                                    |
| Tests       | JUnit 5 · Mockito · AssertJ                  |

---

## Project Structure

```
src/main/java/com/hackconnect/
├── HackConnectApplication.java       ← Entry point
├── config/
│   ├── SecurityConfig.java           ← JWT stateless auth, CORS, role-based rules
│   └── DataSeeder.java               ← Seeds 8 opportunities + 4 roadmaps on startup
├── controller/
│   ├── AuthController.java           ← POST /api/v1/auth/register | /login
│   ├── OpportunityController.java    ← Full CRUD + filter + verify
│   └── RoadmapController.java        ← CRUD + enroll + step progress
├── service/
│   ├── AuthService.java              ← Register, login, JWT issuance
│   ├── OpportunityService.java       ← Filter, CRUD, view tracking, verify
│   └── RoadmapService.java           ← CRUD, enroll, step status, my-roadmaps
├── repository/                       ← Spring Data JPA repos (custom JPQL queries)
├── model/                            ← JPA entities: User, Opportunity, LearningRoadmap,
│                                         RoadmapStep, UserRoadmapProgress
├── dto/
│   ├── request/                      ← AuthRequest, OpportunityRequest, RoadmapRequest
│   └── response/                     ← ApiResponse<T>, AuthResponse, OpportunityResponse,
│                                         RoadmapResponse (Summary | Detail | EnrolledSummary)
├── security/
│   ├── JwtTokenProvider.java         ← Generate & validate JWT
│   ├── JwtAuthFilter.java            ← OncePerRequestFilter
│   └── UserDetailsServiceImpl.java   ← Load user from DB for Spring Security
└── exception/
    ├── HackConnectException.java     ← Typed exceptions (NotFound, Duplicate, Forbidden…)
    └── GlobalExceptionHandler.java   ← @RestControllerAdvice → uniform ApiResponse errors
```

---

## Running Locally (Dev)

```bash
# 1. Clone and enter project
cd hackconnect

# 2. Run with H2 in-memory database
mvn spring-boot:run

# App starts at:   http://localhost:8080
# H2 Console:      http://localhost:8080/h2-console
#   JDBC URL:      jdbc:h2:mem:hackconnectdb
#   User: sa  |  Password: (empty)
```

The `DataSeeder` automatically inserts:
- 3 users (admin, 2 students)
- 8 opportunities (3 hackathons, 2 competitions, 2 internships, 1 event)
- 4 roadmaps with full steps (Full Stack, Frontend, ML, Backend)

---

## Running in Production

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/hackconnect
export DB_USER=hackconnect
export DB_PASS=your_db_password
export JWT_SECRET=your-256-bit-secret-minimum-32-chars

# Build
mvn clean package -DskipTests

# Run with prod profile
java -jar target/hackconnect-1.0.0.jar --spring.profiles.active=prod
```

---

## API Reference

### Authentication

All protected endpoints require: `Authorization: Bearer <token>`

| Method | Path                        | Auth    | Description                  |
|--------|-----------------------------|---------|------------------------------|
| POST   | `/api/v1/auth/register`     | Public  | Register new student         |
| POST   | `/api/v1/auth/login`        | Public  | Login → JWT token            |

**Register body:**
```json
{
  "name": "Priyansh Sharma",
  "email": "priyansh@iit.ac.in",
  "password": "Secret@1234",
  "college": "IIT Delhi",
  "graduationYear": "2025"
}
```

**Login body:**
```json
{ "email": "priyansh@iit.ac.in", "password": "Secret@1234" }
```

**Response (both):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "name": "Priyansh Sharma",
    "email": "priyansh@iit.ac.in",
    "role": "STUDENT"
  }
}
```

---

### Opportunities

| Method | Path                              | Auth          | Description                            |
|--------|-----------------------------------|---------------|----------------------------------------|
| GET    | `/api/v1/opportunities`           | Public        | Filter + paginate opportunities        |
| GET    | `/api/v1/opportunities/upcoming`  | Public        | Soonest deadlines (default: top 6)     |
| GET    | `/api/v1/opportunities/trending`  | Public        | Top 6 verified by view count           |
| GET    | `/api/v1/opportunities/domains`   | Public        | All distinct domain values             |
| GET    | `/api/v1/opportunities/{id}`      | Public        | Get by id (increments view count)      |
| POST   | `/api/v1/opportunities`           | Any user      | Create opportunity (unverified)        |
| PUT    | `/api/v1/opportunities/{id}`      | Owner / Admin | Update opportunity                     |
| DELETE | `/api/v1/opportunities/{id}`      | Owner / Admin | Delete opportunity                     |
| PATCH  | `/api/v1/opportunities/{id}/verify` | ADMIN only  | Mark as verified                       |

**Filter query parameters:**
```
?type=HACKATHON          # HACKATHON | COMPETITION | INTERNSHIP | EVENT | WORKSHOP
&domain=Web Dev
&online=true
&verified=true
&search=react            # searches title, organizer, domain
&upcomingOnly=true       # deadline > now
&page=0&size=10
&sortBy=deadline         # deadline (default) | views | newest | title
```

---

### Learning Roadmaps

| Method | Path                                          | Auth       | Description                              |
|--------|-----------------------------------------------|------------|------------------------------------------|
| GET    | `/api/v1/roadmaps`                            | Public*    | List all (with progress if authenticated)|
| GET    | `/api/v1/roadmaps/popular`                    | Public*    | Top 6 by enrollment                      |
| GET    | `/api/v1/roadmaps/domains`                    | Public     | All distinct domains                     |
| GET    | `/api/v1/roadmaps/my`                         | Required   | My enrolled roadmaps + progress          |
| GET    | `/api/v1/roadmaps/{id}`                       | Public*    | Full detail with steps + progress        |
| POST   | `/api/v1/roadmaps/{id}/enroll`                | Required   | Enroll (creates step progress rows)      |
| PATCH  | `/api/v1/roadmaps/{roadmapId}/steps/{stepId}/status` | Required | Update step status               |
| POST   | `/api/v1/roadmaps`                            | ADMIN only | Create roadmap with steps                |
| DELETE | `/api/v1/roadmaps/{id}`                       | ADMIN only | Delete roadmap                           |

*Public but returns enriched data (progress %) when authenticated.

**Filter query parameters (GET /api/v1/roadmaps):**
```
?domain=Frontend
&level=BEGINNER          # BEGINNER | INTERMEDIATE | ADVANCED
```

**Step status update body:**
```json
{ "status": "COMPLETED" }   // NOT_STARTED | IN_PROGRESS | COMPLETED
```

**Enrolled roadmaps response:**
```json
{
  "success": true,
  "data": [
    {
      "roadmapId": 1,
      "title": "Full Stack Web Development",
      "domain": "Full Stack",
      "level": "BEGINNER",
      "totalSteps": 6,
      "completedSteps": 3,
      "progressPercent": 50,
      "enrolledAt": "2024-03-15T10:30:00"
    }
  ]
}
```

---

## Default Seed Credentials

| Role    | Email                       | Password       |
|---------|-----------------------------|----------------|
| ADMIN   | admin@hackconnect.dev       | Admin@1234     |
| STUDENT | priyansh@iit.ac.in          | Student@1234   |
| STUDENT | ananya@bits.ac.in           | Student@1234   |

---

## Error Responses

All errors follow the same envelope:

```json
{
  "success": false,
  "error": "Opportunity not found with id: 99",
  "path": "/api/v1/opportunities/99",
  "timestamp": "2024-03-15T14:22:31"
}
```

| HTTP Status | Meaning                                   |
|-------------|-------------------------------------------|
| 400         | Validation error / bad request body       |
| 401         | Invalid or missing JWT                    |
| 403         | Not owner or not ADMIN                    |
| 404         | Resource not found                        |
| 409         | Duplicate (email, or already enrolled)    |
| 500         | Unexpected server error (logged)          |

---

## Running Tests

```bash
mvn test
```

Covers:
- `AuthServiceTest`      — register, login, duplicate email
- `OpportunityServiceTest` — filter, getById, create, verify, upcoming
- `RoadmapServiceTest`   — getById, enroll, duplicate enroll, step status, create
