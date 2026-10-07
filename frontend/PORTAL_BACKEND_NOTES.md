# SmartMove portals: backend integration plan

The public website, authentication screens, passenger portal, driver portal, and admin workspace currently use shared `mockData.js` fixtures plus browser `localStorage` for demo records. This is not a connected Spring Boot or Oracle service. One shared demo `users` collection holds `userId`, `username`, `email`, `role`, `accountStatus`, and `linkedProfileId`; passenger, driver, and admin profile records remain separate. Allowed roles are `PASSENGER`, `DRIVER`, `ADMIN`, and `SUPER_ADMIN`. Unified demo sign-in looks up email/username and determines role from that account record. Password text is required by the login UI, explicitly not verified, and never persisted. Role guards are client UX only, not access controls. No secret or payment-card data is persisted.

## Demo identities and account lifecycle

- Initial SUPER_ADMIN: username `superadmin`, email `admin@smartmove.lk`, linked to `ADM-001`.
- Initial ordinary ADMIN: username `operations`, email `operations@smartmove.lk`, linked to `ADM-002`.
- Existing passenger profiles are compatible demo accounts. Their usernames default to their email local part; use the profile email or username displayed on the unified login screen.
- Existing driver profiles are migrated to demo accounts. If a legacy driver profile has no email, its username defaults to its profile ID (for example `dr-201`) and its synthetic address is `{username}@driver.smartmove.demo`. The login screen lists accounts and usernames.
- Admin-provisioned drivers and admins get an active demo account without a generated password. The demo sign-in notice explicitly explains that any non-empty password field is not checked. This is not account activation or secure authentication.
- Passengers self-register with a separate profile and unified account. Driver/admin account creation is not publicly available. Only SUPER_ADMIN can manage other administrator accounts; all admin levels use `/admin`.

In production the common Oracle `USERS` table should store password hashes (not browser-side secrets) and link to `PASSENGERS` or `DRIVERS` through a constrained role/profile relationship. ADMIN and SUPER_ADMIN records link to administrator profiles or an equivalent staff table.

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
| `POST` | `/api/admin/drivers` | Create a driver account/profile. Require ADMIN or SUPER_ADMIN authorization. |
| `GET` | `/api/admin/drivers` | List drivers for authorized admins. |
| `PATCH` | `/api/admin/drivers/{id}` | Update an authorized driver's profile. |
| `PATCH` | `/api/admin/drivers/{id}/account-status` | Activate/disable a driver account under backend policy. |
| `GET` | `/api/admin/accounts` | List administrator accounts. Require SUPER_ADMIN. |
| `POST` | `/api/admin/accounts` | Create an ADMIN account/profile. Require SUPER_ADMIN; never accept public role elevation. |
| `PATCH` | `/api/admin/accounts/{id}` | Update administrator profile details. Require SUPER_ADMIN. |
| `PATCH` | `/api/admin/accounts/{id}/status` | Activate/disable an administrator account. Require SUPER_ADMIN and protect the last active SUPER_ADMIN. |

## Required security and integration work

- **TODO:** implement Spring Boot authentication; hash and verify passwords with a suitable adaptive password-hashing algorithm and never return/store password material in the client.
- **TODO:** choose secure JWT/session and refresh-token handling, expiry, revocation, CSRF/CORS policy, and secure transport. Do not reuse this demo's browser role flag as an authorization mechanism.
- **TODO:** enforce passenger/driver/admin role authorization and ownership on every protected backend endpoint; validate driver assignment and booking ownership server-side.
- **TODO:** model Oracle `USERS` with unique normalized username/email, role enum (`PASSENGER`, `DRIVER`, `ADMIN`, `SUPER_ADMIN`), account status, password hash and profile foreign key; enforce role/profile consistency with constraints and service validation.
- **TODO:** provision the initial SUPER_ADMIN only through a controlled backend bootstrap using environment-managed credentials or a secure one-time setup. Disable bootstrap after setup; never hardcode a production password or offer public SUPER_ADMIN registration.
- **TODO:** enforce SUPER_ADMIN-only admin account management, ADMIN/SUPER_ADMIN driver provisioning, account status rules, no self-disable, and preservation of at least one active SUPER_ADMIN in backend transactions.
- **TODO:** make booking creation and seat allocation atomic to prevent overbooking; define real operator cancellation/refund rules.
- **TODO:** connect a payment provider only after compliance, reconciliation, webhook verification, and server-side payment-state requirements are defined. This portal currently has no real payment processing.
- **TODO:** replace `AppContext` demo data operations with `api.js` calls, including loading, empty, validation and server-error handling; add API contract and integration tests.
- **TODO:** implement the chosen persistence, notification/email delivery, and audit requirements. None of these services is connected in this workspace.
