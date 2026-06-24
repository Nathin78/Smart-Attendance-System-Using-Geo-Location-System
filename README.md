# Smart Attendance System Using Geo-Location

Full-stack attendance platform where users can mark attendance only when they are inside an allowed geofence.

## Project Structure

- `frontend/` : HTML, CSS, JavaScript client
- `backend/` : Spring Boot REST API + MySQL + JWT

## Tech Stack

- Frontend: HTML, CSS, JavaScript
- Backend: Spring Boot (Java 17), REST API, Spring Security, JWT
- Database: MySQL

## Key Features

- JWT login and registration
- BCrypt password hashing
- GPS-based attendance marking
- Haversine geofence validation
- Prevent duplicate attendance per day (service check + DB unique constraint)
- Attendance status: `PRESENT` / `LATE`
- Admin dashboard with users, reports, and geofence management
- User analytics dashboard with monthly snapshot, streak tracking, and recent activity
- Leave request workflow with admin approval and attendance blocking on approved leave days
- Notification center with unread polling, in-app read actions, and browser notifications
- Profile management with avatar uploads and live save-state feedback
- Refreshed high-contrast frontend design system across landing, auth, dashboard, and profile pages
- Environment-based runtime configuration for DB, CORS, JWT, and admin bootstrap

## Default Admin Account

- Email: `admin@smartattendance.com` (can be overridden with `ADMIN_EMAIL`)
- Password: `Admin@123` (can be overridden with `ADMIN_PASSWORD`)

## Database Tables

Auto-managed by JPA (`ddl-auto=update`):

- `users` (`id`, `name`, `email`, `password`, `role`)
- `attendance` (`id`, `user_id`, `date`, `time`, `latitude`, `longitude`, `status`, `distance_meters`, `created_at`)
- `leave_requests` (`id`, `user_id`, `start_date`, `end_date`, `reason`, `status`, `admin_comment`, `created_at`, `updated_at`)
- `geofence` (`id`, `latitude`, `longitude`, `radius`)

## API Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/attendance/mark`
- `GET /api/attendance/user`
- `GET /api/attendance/summary`
- `POST /api/leave-requests`
- `GET /api/leave-requests/my`
- `GET /api/admin/users`
- `GET /api/admin/reports?date=YYYY-MM-DD`
- `GET /api/admin/leave-requests`
- `PUT /api/admin/leave-requests/{id}`
- `GET /api/admin/geofence`
- `PUT /api/admin/geofence`
- `GET /api/geofence/current`
- `GET /api/notifications/unread`
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `GET /api/profile/me`
- `PUT /api/profile/me`
- `POST /api/profile/me/avatar`

## Environment Variables

Main runtime variables used by the backend:

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `3306`)
- `DB_NAME` (default: `smart_attendance_db`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: `root`)
- `JWT_SECRET` (default demo value, change in real deployment)
- `JWT_EXPIRATION_MS` (default: `86400000`)
- `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`
- `CORS_ALLOWED_ORIGINS` (comma-separated)

## Run Locally

1. Start MySQL.
2. Start backend:

```bash
cd backend
mvn spring-boot:run
```

3. Start frontend (from project root) with any static server, for example:

```bash
python -m http.server 5500 -d frontend
```

4. Open `http://localhost:5500/login.html`.

## Notes

- Frontend API base is resolved at runtime from `js/config.js`:
  - `window.__SMART_API_BASE__` (if provided)
  - `localStorage.sa_api_base` (if set)
  - inferred host fallback (`http://<current-host>:8081`)
- First startup automatically seeds:
  - admin user
  - default geofence (`12.9716`, `77.5946`, radius `300m`)
- The frontend stylesheet is mirrored into `backend/src/main/resources/static/` so the packaged app matches the source frontend.
- JavaScript syntax was checked after the UI refresh to catch obvious client-side errors.
