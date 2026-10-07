# SmartMove portals: backend integration plan

The public website, authentication screens, passenger portal, driver portal, and admin workspace currently use shared `mockData.js` fixtures plus browser `localStorage` for demo records. This is not a connected Spring Boot service. Role guards and the selected demo identity are client-side UX only; they are not access controls. No plaintext password, secret, or payment-card data is persisted.

## Suggested REST contract

All endpoints below are planned only. Resource DTOs, validation, authorization, and persistence must be designed and implemented in the backend before production use.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/passengers/register` | Validate and register a passenger profile. |
| `POST` | `/api/auth/login` | Authenticate a user and issue a secure session/token. |
| `POST` | `/api/auth/logout` | Revoke the active server session/refresh token. |
| `GET` | `/api/auth/me` | Resolve the authenticated profile and role. |
| `GET` | `/api/trips?origin=&destination=&date=&passengers=` | Search scheduled trips and authoritative seat availability. |
| `GET` | `/api/trips/{tripId}` | Return trip, route, vehicle summary, times, fare and availability. |
| `POST` | `/api/bookings` | Create a booking for the authenticated passenger with server-side capacity checks. |
| `GET` | `/api/passengers/me/bookings` | List only the caller's bookings. |
| `GET` | `/api/bookings/{bookingId}/ticket` | Return an authorized ticket/booking reference. |
| `POST` | `/api/bookings/{bookingId}/cancel` | Apply server cancellation rules and release seats. |
| `GET` | `/api/passengers/me/payments` | Read administrative payment records for the caller's bookings. |
| `GET`, `POST` | `/api/passengers/me/reviews` | List and submit eligible trip reviews. |
| `GET` | `/api/announcements?audience=passenger` | List published notices for passengers. |
| `GET` | `/api/drivers/me/trips` | List trips assigned to the authenticated driver only. |
| `GET` | `/api/drivers/me/vehicle` | Return the driver's authorized current vehicle summary. |
| `PATCH` | `/api/driver/trips/{tripId}/status` | Validate and update an assigned trip's operational status. |
| `POST` | `/api/driver/issues` | Submit a vehicle/trip issue for operations follow-up. |
| `GET`, `PATCH` | `/api/drivers/me/profile` | Read/update allowed driver contact fields. |
| `GET`, `PATCH` | `/api/passengers/me/profile` | Read/update the authenticated passenger profile. |

## Required security and integration work

- **TODO:** implement Spring Boot authentication; hash passwords with a suitable adaptive password-hashing algorithm and never return/store password material in the client.
- **TODO:** choose secure JWT/session and refresh-token handling, expiry, revocation, CSRF/CORS policy, and secure transport. Do not reuse this demo's browser role flag as an authorization mechanism.
- **TODO:** enforce passenger/driver/admin role authorization and ownership on every protected backend endpoint; validate driver assignment and booking ownership server-side.
- **TODO:** make booking creation and seat allocation atomic to prevent overbooking; define real operator cancellation/refund rules.
- **TODO:** connect a payment provider only after compliance, reconciliation, webhook verification, and server-side payment-state requirements are defined. This portal currently has no real payment processing.
- **TODO:** replace `AppContext` demo data operations with `api.js` calls, including loading, empty, validation and server-error handling; add API contract and integration tests.
- **TODO:** implement the chosen persistence, notification/email delivery, and audit requirements. None of these services is connected in this workspace.
