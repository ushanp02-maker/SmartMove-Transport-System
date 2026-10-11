-- Run AFTER installing smartmove_booking_payment_operations.sql.
-- Read-only verification: no inserts, updates, deletes, commits or rollbacks.
SET SERVEROUTPUT ON
DECLARE
  v_count NUMBER;
  v_invalid NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM user_objects
    WHERE object_name='SM_BOOKING_PAYMENTS'
      AND object_type IN ('PACKAGE','PACKAGE BODY') AND status='VALID';
  IF v_count<>2 THEN
    RAISE_APPLICATION_ERROR(-20201,'SM_BOOKING_PAYMENTS package/body not both VALID');
  END IF;
  DBMS_OUTPUT.PUT_LINE('PASS: PL/SQL package and body are VALID');
  SELECT COUNT(*) INTO v_invalid FROM user_errors WHERE name='SM_BOOKING_PAYMENTS';
  IF v_invalid>0 THEN RAISE_APPLICATION_ERROR(-20202,'Compilation errors exist'); END IF;
  DBMS_OUTPUT.PUT_LINE('PASS: no PL/SQL compilation errors');
  SELECT COUNT(*) INTO v_count FROM user_procedures
    WHERE object_name='SM_BOOKING_PAYMENTS'
      AND procedure_name IN ('CREATE_BOOKING','CREATE_PAYMENT','CONFIRM_BOOKING',
        'CANCEL_UNPAID_BOOKING','COMPLETE_PAYMENT','FAIL_PAYMENT','REFUND_PAYMENT',
        'DELETE_UNSUCCESSFUL_PAYMENT','MAX_OCCUPIED','SEGMENT_FARE');
  IF v_count<>10 THEN
    RAISE_APPLICATION_ERROR(-20203,'Missing package subprograms: found '||v_count||'/10');
  END IF;
  DBMS_OUTPUT.PUT_LINE('PASS: all 10 PL/SQL operations are present');
END;
/
SELECT name,type,line,position,text FROM user_errors
 WHERE name='SM_BOOKING_PAYMENTS' ORDER BY sequence;
-- Optional: check a live trip segment WITHOUT modifying data:
-- SELECT sm_booking_payments.segment_fare(:trip_id,:boarding_stop_id,:destination_stop_id) FROM dual;
