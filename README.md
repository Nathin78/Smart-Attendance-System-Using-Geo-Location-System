# Smart Attendance System Using Geo-Location

Full-stack attendance platform where users can mark attendance only when they are inside an allowed geofence.

## Project Structure

- `frontend/` : HTML, CSS, JavaScript clie
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

frontend.
- JavaScript syntax was checked after the UI refresh to catch obvious client-side errors.
