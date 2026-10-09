# SmartMove integration verification checklist

This document distinguishes existing API wiring from verified end-to-end behavior. GitHub edits alone do not prove that Spring Boot, Oracle, MongoDB and the browser work together.

## Run
1. Pull main, start Oracle XE and MongoDB, and run BackendApplication in IntelliJ.
2. Start the frontend with VITE_DATA_MODE=API and VITE_API_BASE_URL=http://localhost:8080/api.
3. Run npm ci, npm run lint, npm run build in frontend. Run the backend's Maven tests.
4. Log in as SUPER_ADMIN. Do not publish passwords or application.properties secrets.

## Test by workflow
- [ ] Super Admin / Admin / Passenger / Driver login, logout, role restrictions and refresh behavior.
- [x] Vehicle create, edit and browser-refresh persistence (reported by project owner).
- [ ] Routes: create, edit, ordered intermediate stops, search and trip linking.
- [ ] Trips: assign an existing driver/vehicle, prevent conflicts, publish availability.
- [ ] Passenger: register, search origin/destination including intermediate stops, book and cancel, view tickets.
- [ ] Driver: create driver profile and linked account; see only assigned trips; start/pause/resume/complete.
- [ ] GPS: allow permission on driver browser, submit real updates, display latest fix on admin fleet map and passenger tracking; never simulate positions.
- [ ] Staff: passenger request, admin approval, assignment to a real trip and cancellation.
- [ ] On-demand: map pickup/destination, request, approve, assign and cancel.
- [ ] Payment records: create/complete/fail/refund; no external payment gateway.
- [ ] MongoDB: feedback submission/moderation and announcement creation/publishing.
- [ ] Reports: backend-derived counts/revenue, date filtering and CSV export.
- [ ] Database security: ownership enforcement, account deactivation, capacity and overlap validation.
- [ ] Run automated tests and inspect backend logs for errors.

## Known implementation limits
- The API driver dashboard and GPS sharing are wired, but not end-to-end tested.
- The API admin passenger and booking directories currently support viewing/searching, not full management.
- API passenger trip search, booking, cancellation, tickets, profile, payments, announcements, reviews, on-demand requests, staff requests, tracking and dashboard now have backend-backed screens; their live workflows are unverified.
- API driver schedule, profile, assigned vehicle, trip lifecycle, announcements and GPS have backend-backed screens; driver issue reporting still requires integration.
- GitHub Actions now runs npm lint/build and Maven compile; a successful run is not a substitute for live database tests.
- Staff and on-demand admin actions link existing trips; they do not automatically create recurring trips.
- Some passenger/driver secondary pages still use demo-state components and require API replacements.
- Driver profile creation must be available before linking a DRIVER login to a driverId.
- Live fleet uses authenticated polling every 15 seconds, not a WebSocket feed.
- Browser-based GPS sharing stops when the driver closes the page.
- All new API screens require live authorization and integration tests before submission.

Do not claim the project is fully finished until the unchecked workflows are verified.
