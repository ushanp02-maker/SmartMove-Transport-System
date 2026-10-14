-- Read-only preflight for SmartMove booking/payment PL/SQL.
-- Run in SQL Developer as the SAME Oracle user used by Spring Boot.
-- This script does not modify tables or invoke state-changing procedures.
SET SERVEROUTPUT ON
DECLARE
  v_missing NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_missing FROM (
    SELECT 'BOOKINGS' name FROM dual UNION ALL
    SELECT 'PAYMENTS' FROM dual UNION ALL
    SELECT 'TRIPS' FROM dual
  ) expected WHERE NOT EXISTS (
    SELECT 1 FROM user_tables t WHERE t.table_name=expected.name
  );
  IF v_missing > 0 THEN
    DBMS_OUTPUT.PUT_LINE('FAIL: '||v_missing||' required tables missing from current schema');
  ELSE
    DBMS_OUTPUT.PUT_LINE('PASS: BOOKING, PAYMENT and TRIP tables exist');
  END IF;
END;
/
SELECT object_name, object_type, status
FROM user_objects
WHERE object_name IN ('SM_BOOKING_PAYMENTS','SM_REPORTS')
ORDER BY object_name, object_type;
SELECT name, type, line, position, text
FROM user_errors
WHERE name IN ('SM_BOOKING_PAYMENTS','SM_REPORTS')
ORDER BY name, sequence;
SELECT table_name, column_name, data_type, nullable
FROM user_tab_columns
WHERE table_name IN ('BOOKINGS','PAYMENTS','TRIPS')
  AND column_name IN ('ID','STATUS','AMOUNT','TOTAL_FARE','DEPARTURE_TIME',
                      'BOOKING_ID','CANCELLED_AT','PAYMENT_DATE','REFUNDED_AT',
                      'REFUND_AMOUNT','NOTES')
ORDER BY table_name, column_id;
