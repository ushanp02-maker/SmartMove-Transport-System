-- Phase 1: Oracle-owned booking/payment state transitions.
-- Install with SQL Developer (Run Script/F5) in the SmartMove schema.
-- No data is deleted. This package is NOT yet called by Spring Boot.
-- Do not enable until integration and transaction tests pass.
CREATE OR REPLACE PACKAGE sm_booking_payments AS
  PROCEDURE confirm_booking(p_booking_id IN NUMBER);
  PROCEDURE cancel_unpaid_booking(p_booking_id IN NUMBER);
  PROCEDURE complete_payment(p_payment_id IN NUMBER);
  PROCEDURE fail_payment(p_payment_id IN NUMBER, p_reason IN VARCHAR2);
  PROCEDURE refund_payment(p_payment_id IN NUMBER, p_amount IN NUMBER, p_reason IN VARCHAR2);
END sm_booking_payments;
/
CREATE OR REPLACE PACKAGE BODY sm_booking_payments AS
  PROCEDURE confirm_booking(p_booking_id IN NUMBER) IS
    v_status bookings.status%TYPE;
    v_fare bookings.total_fare%TYPE;
    v_paid NUMBER;
  BEGIN
    SELECT status, total_fare INTO v_status, v_fare
      FROM bookings WHERE id=p_booking_id FOR UPDATE;
    IF v_status='CONFIRMED' THEN RETURN; END IF;
    IF v_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20101,'Only pending bookings can be confirmed');
    END IF;
    SELECT COUNT(*) INTO v_paid FROM payments
      WHERE booking_id=p_booking_id AND status='COMPLETED' AND amount=v_fare;
    IF v_paid=0 THEN
      RAISE_APPLICATION_ERROR(-20102,'A completed matching payment is required');
    END IF;
    UPDATE bookings SET status='CONFIRMED' WHERE id=p_booking_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20103,'Booking not found');
  END;

  PROCEDURE cancel_unpaid_booking(p_booking_id IN NUMBER) IS
    v_status bookings.status%TYPE;
    v_departure trips.departure_time%TYPE;
    v_paid NUMBER;
  BEGIN
    SELECT b.status,t.departure_time INTO v_status,v_departure
      FROM bookings b JOIN trips t ON t.id=b.trip_id
      WHERE b.id=p_booking_id FOR UPDATE OF b.status;
    IF v_status='CANCELLED' THEN RETURN; END IF;
    IF v_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20104,'Only unpaid pending bookings can be cancelled here');
    END IF;
    IF v_departure<=SYSDATE THEN
      RAISE_APPLICATION_ERROR(-20105,'Trip departure time has passed');
    END IF;
    SELECT COUNT(*) INTO v_paid FROM payments
      WHERE booking_id=p_booking_id AND status IN ('COMPLETED','REFUNDED');
    IF v_paid>0 THEN
      RAISE_APPLICATION_ERROR(-20106,'Paid bookings require the refund workflow');
    END IF;
    UPDATE bookings SET status='CANCELLED',cancelled_at=SYSTIMESTAMP WHERE id=p_booking_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20103,'Booking not found');
  END;

  PROCEDURE complete_payment(p_payment_id IN NUMBER) IS
    v_status payments.status%TYPE;
    v_amount payments.amount%TYPE;
    v_booking_id payments.booking_id%TYPE;
    v_booking_status bookings.status%TYPE;
    v_fare bookings.total_fare%TYPE;
    v_trip_status trips.status%TYPE;
    v_departure trips.departure_time%TYPE;
  BEGIN
    SELECT booking_id,status,amount INTO v_booking_id,v_status,v_amount
      FROM payments WHERE id=p_payment_id;
    -- Consistent lock order: booking first, payment second.
    SELECT b.status,b.total_fare,t.status,t.departure_time
      INTO v_booking_status,v_fare,v_trip_status,v_departure
      FROM bookings b JOIN trips t ON t.id=b.trip_id
      WHERE b.id=v_booking_id FOR UPDATE OF b.status;
    SELECT status INTO v_status FROM payments WHERE id=p_payment_id FOR UPDATE;
    IF v_status<>'PENDING' OR v_booking_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20107,'Only pending bookings and payments can be completed');
    END IF;
    IF v_amount<>v_fare THEN
      RAISE_APPLICATION_ERROR(-20108,'Payment amount does not match booking fare');
    END IF;
    IF v_trip_status<>'SCHEDULED' OR v_departure<=SYSDATE THEN
      RAISE_APPLICATION_ERROR(-20109,'Trip departure has passed or trip is unavailable');
    END IF;
    UPDATE payments SET status='COMPLETED',payment_date=SYSTIMESTAMP WHERE id=p_payment_id;
    UPDATE bookings SET status='CONFIRMED' WHERE id=v_booking_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20110,'Payment or booking not found');
  END;

  PROCEDURE fail_payment(p_payment_id IN NUMBER,p_reason IN VARCHAR2) IS
    v_status payments.status%TYPE;
  BEGIN
    SELECT status INTO v_status FROM payments WHERE id=p_payment_id FOR UPDATE;
    IF v_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20111,'Only pending payments can be marked as failed');
    END IF;
    UPDATE payments SET status='FAILED',
      notes=COALESCE(NULLIF(TRIM(p_reason),''),'Simulated payment failed')
      WHERE id=p_payment_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20110,'Payment not found');
  END;

  PROCEDURE refund_payment(p_payment_id IN NUMBER,p_amount IN NUMBER,p_reason IN VARCHAR2) IS
    v_booking_id payments.booking_id%TYPE;
    v_payment_status payments.status%TYPE;
    v_payment_amount payments.amount%TYPE;
    v_trip_status trips.status%TYPE;
    v_amount NUMBER(18,2);
  BEGIN
    IF p_amount IS NULL THEN RAISE_APPLICATION_ERROR(-20112,'Refund amount is required'); END IF;
    v_amount:=ROUND(p_amount,2);
    SELECT booking_id INTO v_booking_id FROM payments WHERE id=p_payment_id;
    SELECT t.status INTO v_trip_status FROM bookings b JOIN trips t ON t.id=b.trip_id
      WHERE b.id=v_booking_id FOR UPDATE OF b.status;
    SELECT status,amount INTO v_payment_status,v_payment_amount
      FROM payments WHERE id=p_payment_id FOR UPDATE;
    IF v_payment_status<>'COMPLETED' THEN
      RAISE_APPLICATION_ERROR(-20113,'Only completed payments can be refunded');
    END IF;
    IF v_amount<=0 OR v_amount>v_payment_amount THEN
      RAISE_APPLICATION_ERROR(-20114,'Refund must be greater than zero and cannot exceed payment');
    END IF;
    IF v_trip_status='COMPLETED' THEN
      RAISE_APPLICATION_ERROR(-20115,'Completed trips cannot be refunded through this workflow');
    END IF;
    UPDATE payments SET status='REFUNDED',refund_amount=v_amount,
      refunded_at=SYSTIMESTAMP,notes=COALESCE(TRIM(p_reason),'Refund processed')
      WHERE id=p_payment_id;
    UPDATE bookings SET status='CANCELLED',cancelled_at=SYSTIMESTAMP WHERE id=v_booking_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20110,'Payment or booking not found');
  END;
END sm_booking_payments;
/
SHOW ERRORS PACKAGE sm_booking_payments
SHOW ERRORS PACKAGE BODY sm_booking_payments
