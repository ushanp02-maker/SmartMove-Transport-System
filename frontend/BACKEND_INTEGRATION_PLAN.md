# SmartMove backend integration plan

**Status: proposal only.** The React application defaults to `VITE_DATA_MODE=DEMO` and uses the shared `AppContext` and mock collections. No Spring Boot service, Oracle database, MongoDB store, email, payment gateway, geocoder, or live location service is connected. `VITE_DATA_MODE=API` enables the centralized HTTP client modules only; it does not make the endpoints below exist. Use `.env.example` as a template.

## Frontend integration boundary

- `services/apiClient.js` owns the configured base URL and HTTP error handling. Service modules wrap domain calls; React pages should not call `fetch` directly.
- Set `VITE_DATA_MODE=API`, `VITE_API_BASE_URL`, and optionally `VITE_WS_BASE_URL` only after a compatible service is deployed. The frontend must not assume localhost.
- API mode is deliberately opt-in. Most current pages still run the demo repository through AppContext; API service wrappers are contracts/preparation, not a connected backend or automatic demo-to-server fallback.
- Client route guards and assignment checks are usability checks only. Spring Boot must repeat all ownership, role, eligibility, capacity, overlap, status and authorization validation.
- Demo map selection uses OpenStreetMap tiles and a curated list of supported place landmarks. Map clicks are browser-memory values. Submitted demo custom requests round coordinates to two decimal places (roughly kilometre precision); raw geolocation samples are not persisted.

## Proposed REST contract

All routes below are **proposed**, not implemented APIs. Use JSON DTOs, stable ISO-8601 timestamps, normalized identifiers, pagination and consistent error responses (for example `{code, message, fieldErrors}`). API methods shown are suggestions and need agreement with the backend team.

### Authentication and profiles

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Public. `{identity,password}`; verify password hash and return the account role plus secure session/token. |
| `POST` | `/api/auth/register` | Public passenger self-registration only. Never accept caller-selected DRIVER/ADMIN/SUPER_ADMIN. |
| `POST` | `/api/auth/logout` | Authenticated; revoke session/refresh token. |
| `GET` | `/api/auth/me` | Authenticated; return account and permitted linked profile. |

### Locations, routes and scheduled trips

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `GET` | `/api/routes` | Public active routes, optionally filtered by service type. |
| `GET` | `/api/routes/search?from=&to=&date=&serviceType=&passengers=` | Public search through ordered origin/destination/intermediate stops and scheduled trips. |
| `GET` | `/api/routes/{id}/stops` | Public route stop order, coordinates, and scheduled stop times. |
| `GET` | `/api/stops/nearby?latitude=&longitude=&radiusMetres=` | Public supported boarding stops near an approximate point. |
| `GET` | `/api/trips?routeId=&date=&serviceType=` | Public scheduled trip summaries and authoritative seat availability. |
| `GET` | `/api/trips/{id}` | Public trip, stop timetable, vehicle summary, service type, fare and availability. |
| `POST` | `/api/bookings` | Passenger only; atomically validate ordered stops, capacity and fare before booking. |

Route search response should include matching `routeId`, `serviceType`, route direction, ordered stops, selected `boardingStopId`/`destinationStopId`, stop times when defined, matching trips, fare and available seats. The service must reject boarding at or after the selected destination in the direction of travel.

### Staff transportation

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `GET` | `/api/staff/routes` | Authenticated employee; backend filters to eligible companies/routes. Admin access may list all. |
| `GET` | `/api/staff/trips?companyId=&date=` | Employee sees only eligible staff schedules; admins see authorized operational scope. |
| `POST` | `/api/staff/bookings` | Eligible employee only; validate company membership, route eligibility, trip, capacity and duplication. |
| `GET` | `/api/staff/bookings/me` | List only the authenticated employee's reservations. |
| `POST` | `/api/admin/staff/routes` | ADMIN/SUPER_ADMIN creates a staff route and ordered boarding stops. |
| `PATCH` | `/api/admin/staff/routes/{id}` | ADMIN/SUPER_ADMIN updates company, stops, operating days, schedule and assignment. |
| `PATCH` | `/api/admin/staff/employees/{id}/eligibility` | ADMIN/SUPER_ADMIN maintains the company employee eligibility roster. |

Staff route data needs `serviceType=STAFF`, `companyId`, company/workplace identity, eligibility metadata, assigned vehicle/driver, operating days and ordered boarding stops. Do not expose one employer's routes or employee roster to another company's users.

### Custom trip requests

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `POST` | `/api/custom-trips` | Passenger submits a request, not a confirmed booking. |
| `GET` | `/api/custom-trips/my` | Passenger lists only their own requests. |
| `GET` | `/api/admin/custom-trips?status=` | ADMIN/SUPER_ADMIN operations queue. |
| `PATCH` | `/api/admin/custom-trips/{id}/status` | ADMIN/SUPER_ADMIN approve/reject/cancel with transition validation and notes. |
| `POST` | `/api/admin/custom-trips/{id}/assign` | ADMIN/SUPER_ADMIN assigns a suitable available vehicle/driver and schedule. |

Proposed custom request body: `pickup`/`destination` place labels and optional coordinates, `travelDate`, `preferredTime`, `passengers`, `vehiclePreference`, `specialRequirements`. Suggested statuses: `PENDING`, `APPROVED`, `REJECTED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`. New requests start `PENDING`; only an assignment transaction should create a dispatchable trip. Assignment must atomically check vehicle active/capacity, driver status, date/time overlap, and concurrent reservation state.

### Driver operations and tracking

| Method | Endpoint | Purpose / access |
| --- | --- | --- |
| `GET` | `/api/drivers/me/trips` | Authenticated driver; assigned trips only. |
| `GET` | `/api/drivers/me/vehicle` | Authenticated driver; assigned vehicle only. |
| `PATCH` | `/api/driver/trips/{id}/status` | Assigned driver only; validate status transition. |
| `POST` | `/api/driver/issues` | Authenticated driver submits a service/vehicle report. |
| `POST` | `/api/trips/{id}/location` | Assigned driver on authorized active trip only. Payload: `{latitude,longitude,accuracy,recordedAt}`. |
| `GET` | `/api/trips/{id}/location` | Passenger with booking/custom-request ownership, assigned driver or authorized admin; return last accepted point and freshness. |
| `GET` | `/api/admin/fleet/locations?serviceType=` | ADMIN/SUPER_ADMIN; only active fleet positions within their authorized scope. |
| WebSocket | `/ws/trips/{id}/locations` (suggested) | Authenticated, authorized subscribers receive validated location events. |

The browser tracking service asks for permission only after the driver explicitly starts tracking an assigned `IN_PROGRESS` trip. It requests updates on movement of at least 100 m or after 15 seconds, ignores poor fixes (>80 m accuracy), and stops on user action, trip end, component teardown, or page close. Demo mode displays a local, ephemeral fix only in the active page; it is not sent or saved. A browser cannot guarantee tracking while backgrounded, locked, or terminated. Reliable background GPS may require a native/mobile implementation. Define retention, precision, consent, privacy and audit rules before persisting any driver location history.

### Existing portal contracts to retain

Keep the existing authentication, passengers, bookings/tickets/cancellation, administrative payment records, reviews, announcements, maintenance, vehicle/driver administration, account management and reports contracts documented in `PORTAL_BACKEND_NOTES.md`. Payment rows remain administrative records until an actual compliant payment provider is selected.

## Expected API states and failures

- `400` field validation / invalid stop order, time or passenger count.
- `401` missing/expired authentication; `403` role, company eligibility, ownership or driver assignment denied.
- `404` missing trip, stop, profile or request.
- `409` concurrent seat capacity, duplicate reservation, vehicle/driver overlap, invalid lifecycle transition or stale update.
- `422` business rule such as inactive vehicle, insufficient capacity, unavailable driver or ineligible employee.
- `429` location update rate limit.
- Network/timeouts and `5xx`: keep form values, expose retry/unavailable state, never invent location or booking success.
- Tracking responses include a server timestamp and stale/offline indicator; a location older than an agreed threshold must not be rendered as live.

## Suggested Oracle relational entities

- `USERS(user_id, username_normalized UNIQUE, email_normalized UNIQUE, password_hash, role, account_status, passenger_id NULL, driver_id NULL, admin_profile_id NULL, created_at, updated_at)` with role/profile consistency checks; include `PASSENGER`, `DRIVER`, `ADMIN`, `SUPER_ADMIN`.
- Existing `PASSENGERS`, `DRIVERS`, `ADMIN_PROFILES`, `VEHICLES`, `ROUTES`, `TRIPS`, `BOOKINGS`, `PAYMENTS`, `REVIEWS`, `MAINTENANCE`, `ANNOUNCEMENTS`.
- `ROUTE_STOPS(route_stop_id, route_id, stop_order, stop_name, latitude, longitude, scheduled_offset_or_time)` with unique `(route_id, stop_order)` and route direction validation.
- `STAFF_COMPANIES(company_id, name, workplace_location_id)`, `STAFF_EMPLOYEES(employee_id, passenger_id, company_id, employee_code, eligibility_status)`, `STAFF_ROUTES(route_id, company_id, assigned_vehicle_id, assigned_driver_id, operating_days)`, `STAFF_BOOKINGS(staff_booking_id, trip_id, employee_id, seats, status)`.
- `CUSTOM_TRIP_REQUESTS(request_id, passenger_id, pickup_label, pickup_latitude, pickup_longitude, destination_label, destination_latitude, destination_longitude, requested_at, travel_date, preferred_time, passenger_count, vehicle_preference, requirements, status, notes, assigned_vehicle_id, assigned_driver_id, assigned_trip_id, version)`.
- Booking/assignment operations need transactions, locking or equivalent concurrency control and auditable state transitions.

## MongoDB responsibilities to decide

Use MongoDB only if product requirements need flexible telemetry/event retention at volume. A possible collection is `vehicle_location_events(tripId, vehicleId, driverId, latitude, longitude, accuracy, recordedAt, receivedAt)` with TTL/retention, indexes by trip/time and authorization-mediated reads. Oracle should remain authoritative for users, route/stop order, trips, assignments, staff eligibility, bookings and request status unless architecture review decides otherwise. Define a location retention period, coarse passenger-visible location policy, consent, access auditing, deletion/retention requirements and cross-store event consistency before implementing. MongoDB is not currently connected.

## Bootstrap, auth and authorization requirements

- Hash/verify passwords server-side with an adaptive algorithm. Never store passwords in frontend localStorage or DTOs.
- Securely bootstrap one initial SUPER_ADMIN through controlled environment-managed setup; disable bootstrap after setup.
- Enforce passenger ownership, employee/company eligibility, assigned-driver checks and ADMIN/SUPER_ADMIN permissions server-side.
- Keep API secrets and credentials out of Vite `VITE_*` variables; Vite variables are public client configuration.
- Define secure cookie/JWT handling, revocation, CSRF/CORS, rate limits, audit logging, WebSocket authorization and error DTOs before production.
- This document is a starting contract only. Endpoint authorization, payload semantics, schedule/time-zone handling, company eligibility policy, GIS provider, location retention and Oracle/MongoDB boundaries require backend/product review.
