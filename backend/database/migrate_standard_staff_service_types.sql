-- Run once as the SmartMove application schema owner in Oracle SQL Developer.
-- Safe for existing route/trip/booking IDs: only changes service-type labels.
-- Existing internally generated CUSTOM routes are intentionally preserved.
-- Review any EXPRESS/PRIVATE/SPECIAL routes manually before using the new UI.
UPDATE ROUTES
SET SERVICE_TYPE = 'STANDARD'
WHERE UPPER(TRIM(SERVICE_TYPE)) = 'COMMUTER';

COMMIT;

-- Check that every normal route uses STANDARD or STAFF.
SELECT ID, NAME, ORIGIN, DESTINATION, SERVICE_TYPE, STATUS
FROM ROUTES
ORDER BY ID;

-- Verify the October 13 scheduled trip remains linked to the same route.
SELECT t.ID AS TRIP_ID, t.STATUS AS TRIP_STATUS,
       t.DEPARTURE_TIME, r.SERVICE_TYPE, r.ORIGIN, r.DESTINATION
FROM TRIPS t
JOIN ROUTES r ON r.ID = t.ROUTE_ID
ORDER BY t.DEPARTURE_TIME;
