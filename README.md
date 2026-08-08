# ⏱️ Workforce Time Tracker & Attendance Auditor

A lightweight backend service designed for logging employee shifts, managing leave records, and automating attendance audits. Built with focus on clean architecture, strict layer isolation, and automated quality gates.

---

## Tech Stack & Key Specs

* **Core Framework:** Spring Boot 4.0.6 (Data JPA, Web MVC, Security, Validation)
* **Language Runtime:** Java 25
* **Database & Migrations:** PostgreSQL, Flyway Migration
* **Security:** JWT Authentication
* **API Documentation:** Swagger UI
* **DevOps:** Integrated Docker & `spring-boot-docker-compose` lifecycle binding
* **Code Quality & Testing:** JUnit 5, Mockito, MockMvc, JaCoCo, Checkstyle

---

## Key Functionality

### Employee Workspace
* **Shift Lifecycle:** Operations for managing shift status (Start ➡️ Pause ➡️ Resume ➡️ End).
* **Cascade Break Closure:** Automatically closes active breaks when a shift is stopped to protect data integrity.
* **Financial Export:** On-demand download of monthly wage reports as Excel-ready CSV files (embedded with UTF-8 BOM marker for clean encoding).

### HR & Administrative Controls
* **Username-Driven Management:** Manual shift corrections and absence registration via a simple string `username` parameter.
* **Overlap Protection:** Hard blocking mechanisms preventing overlapping absence records or conflicting work shifts.
* **Consolidated Reports:** Attendance reports aggregating cross-company data.

### Automated Auditor (`AuditScheduler`)
* Triggered automatically via a cron daemon every weekday at 20:00:
  * **Auto-Closure:** Force-closes forgotten unsubmitted shifts and writes a 60-minute penalty flag onto abandoned breaks.
  * **Truancy Alert:** Tags missed work days with a `FORGOTTEN_START_ALERT` flag for administrative screening.
  * **Leave Awareness:** Automatically detects official active leave records (vacations/sick leaves) and logs them as non-penalizing `APPROVED_ABSENCE` blocks instead of truant entries.
  * **Retroactive Support:** Allows administrators to register leaves retroactively over past truant marks without locking the system workflow.

---

### Run the application via Docker Compose
Open Windows PowerShell in the root directory and execute:
```powershell
docker compose up --build
```

---

## API Documentation & Testing

Once the containers are successfully initialized, the interactive documentation endpoint is available at:
`http://localhost:8080/swagger-ui.html`
* *Note: Pre-configured credentials for both Administrator and Regular User accounts are explicitly documented right inside the description of this `/api/v1/auth/login` endpoint.*
### How to use Authorized Endpoints in Swagger:
1. Generate a valid token using the Auth Controller (`/api/v1/auth/login`).
2. Click the **Authorize** 🔓 button in the top right corner of the Swagger page.
3. Paste your raw token into the **Value** field (Swagger prepends `Bearer` automatically).
4. Click **Authorize** and close the dialog. Secure endpoints (like `/api/v1/shifts`) are now ready to be tested!

---
