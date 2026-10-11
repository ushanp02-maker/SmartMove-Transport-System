-- Phase 1: Oracle-owned booking/payment state transitions.
-- Install with SQL Developer (Run Script/F5) in the SmartMove schema.
-- No data is deleted. This package is NOT yet called by Spring Boot.
-- Do not enable until integration and transaction tests pass.
CREATE OR REPLACE PACKAGE sm_booking_payments AS
  FUNCTION max_occupied(p_trip_id IN NUMBER,p_start_order IN NUMBER,p_end_order IN NUMBER) RETURN NUMBER;
  FUNCTION segment_fare(p_trip_id IN NUMBER,p_board_id IN NUMBER,p_dest_id IN NUMBER) RETURN NUMBER;
  PROCEDURE create_booking(p_passenger_id IN NUMBER,p_trip_id IN NUMBER,p_board_id IN NUMBER,
    p_dest_id IN NUMBER,p_seats IN NUMBER,p_booking_id OUT NUMBER);
  PROCEDURE create_payment(p_booking_id IN NUMBER,p_method IN VARCHAR2,p_notes IN VARCHAR2,p_payment_id OUT NUMBER);
  PROCEDURE delete_unsuccessful_payment(p_payment_id IN NUMBER);
  PROCEDURE confirm_booking(p_booking_id IN NUMBER);
  PROCEDURE cancel_unpaid_booking(p_booking_id IN NUMBER);
  PROCEDURE complete_payment(p_payment_id IN NUMBER);
  PROCEDURE fail_payment(p_payment_id IN NUMBER, p_reason IN VARCHAR2);
  PROCEDURE refund_payment(p_payment_id IN NUMBER, p_amount IN NUMBER, p_reason IN VARCHAR2);
END sm_booking_payments;
/
CREATE OR REPLACE PACKAGE BODY sm_booking_payments AS

  FUNCTION max_occupied(p_trip_id IN NUMBER,p_start_order IN NUMBER,p_end_order IN NUMBER)
    RETURN NUMBER IS
    v_max NUMBER:=0;
    v_occupied NUMBER;
  BEGIN
    IF p_start_order IS NULL OR p_end_order IS NULL OR p_start_order>=p_end_order THEN
      RAISE_APPLICATION_ERROR(-20120,'Invalid route stop order');
    END IF;
    FOR segment IN p_start_order..p_end_order-1 LOOP
      SELECT NVL(SUM(b.seat_count),0) INTO v_occupied
        FROM bookings b
        JOIN route_stops s1 ON s1.id=b.boarding_stop_id
        JOIN route_stops s2 ON s2.id=b.destination_stop_id
        WHERE b.trip_id=p_trip_id AND b.status IN ('PENDING','CONFIRMED')
          AND s1.stop_order<=segment AND s2.stop_order>segment;
      v_max:=GREATEST(v_max,v_occupied);
    END LOOP;
    RETURN v_max;
  END;

  FUNCTION segment_fare(p_trip_id IN NUMBER,p_board_id IN NUMBER,p_dest_id IN NUMBER)
    RETURN NUMBER IS
    v_fare NUMBER; v_distance NUMBER; v_start NUMBER; v_end NUMBER;
  BEGIN
    SELECT t.fare,r.distance_km,s1.distance_from_start_km,s2.distance_from_start_km
      INTO v_fare,v_distance,v_start,v_end
      FROM trips t JOIN routes r ON r.id=t.route_id
      JOIN route_stops s1 ON s1.id=p_board_id AND s1.route_id=r.id
      JOIN route_stops s2 ON s2.id=p_dest_id AND s2.route_id=r.id
      WHERE t.id=p_trip_id;
    IF v_distance IS NULL OR v_distance<=0 OR v_start IS NULL OR v_end IS NULL THEN
      RAISE_APPLICATION_ERROR(-20121,'Route distance information is incomplete');
    END IF;
    IF v_end<=v_start OR v_end-v_start>v_distance THEN
      RAISE_APPLICATION_ERROR(-20122,'Invalid route segment distance');
    END IF;
    RETURN ROUND(v_fare*(v_end-v_start)/v_distance,2);
  EXCEPTION WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20123,'Trip or route stops not found');
  END;

  PROCEDURE create_booking(p_passenger_id IN NUMBER,p_trip_id IN NUMBER,p_board_id IN NUMBER,
    p_dest_id IN NUMBER,p_seats IN NUMBER,p_booking_id OUT NUMBER) IS
    v_status trips.status%TYPE; v_departure trips.departure_time%TYPE;
    v_route_status routes.status%TYPE; v_capacity vehicles.seating_capacity%TYPE;
    v_route_id NUMBER; v_board_order NUMBER; v_dest_order NUMBER;
    v_fare NUMBER; v_count NUMBER; v_payment_id NUMBER;
  BEGIN
    IF p_seats IS NULL OR p_seats<1 OR p_seats>20 OR p_seats<>TRUNC(p_seats) THEN
      RAISE_APPLICATION_ERROR(-20124,'Seat count must be between 1 and 20');
    END IF;
    -- Lock trip to serialize competing reservations.
    SELECT t.status,t.departure_time,r.status,v.seating_capacity,r.id
      INTO v_status,v_departure,v_route_status,v_capacity,v_route_id
      FROM trips t JOIN routes r ON r.id=t.route_id
      JOIN vehicles v ON v.id=t.vehicle_id
      WHERE t.id=p_trip_id FOR UPDATE OF t.status;
    IF v_status<>'SCHEDULED' THEN
      RAISE_APPLICATION_ERROR(-20125,'This trip is not open for booking');
    END IF;
    IF v_departure<=LOCALTIMESTAMP THEN
      RAISE_APPLICATION_ERROR(-20126,'Booking is closed for this trip');
    END IF;
    IF v_route_status<>'ACTIVE' THEN
      RAISE_APPLICATION_ERROR(-20127,'The route is not currently active');
    END IF;
    SELECT COUNT(*) INTO v_count FROM passengers WHERE id=p_passenger_id;
    IF v_count=0 THEN RAISE_APPLICATION_ERROR(-20128,'Passenger not found'); END IF;
    SELECT stop_order INTO v_board_order FROM route_stops
      WHERE id=p_board_id AND route_id=v_route_id;
    SELECT stop_order INTO v_dest_order FROM route_stops
      WHERE id=p_dest_id AND route_id=v_route_id;
    IF v_board_order>=v_dest_order THEN
      RAISE_APPLICATION_ERROR(-20120,'Boarding stop must precede destination');
    END IF;
    IF max_occupied(p_trip_id,v_board_order,v_dest_order)+p_seats>v_capacity THEN
      RAISE_APPLICATION_ERROR(-20129,'Not enough seats are available');
    END IF;
    v_fare:=segment_fare(p_trip_id,p_board_id,p_dest_id)*p_seats;
    INSERT INTO bookings(passenger_id,trip_id,boarding_stop_id,destination_stop_id,
      seat_count,total_fare,status,booking_reference,booked_at)
      VALUES(p_passenger_id,p_trip_id,p_board_id,p_dest_id,p_seats,v_fare,
      'PENDING','SM-'||UPPER(RAWTOHEX(SYS_GUID())),LOCALTIMESTAMP)
      RETURNING id INTO p_booking_id;
    INSERT INTO payments(booking_id,amount,currency,payment_method,status,
      transaction_reference,notes,created_at)
      VALUES(p_booking_id,ROUND(v_fare,2),'LKR','SIMULATED','PENDING',
      'PAY-'||UPPER(RAWTOHEX(SYS_GUID())),'Awaiting payment confirmation',LOCALTIMESTAMP);
  EXCEPTION
    WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20130,'Trip or route stop not found');
  END;

  PROCEDURE create_payment(p_booking_id IN NUMBER,p_method IN VARCHAR2,p_notes IN VARCHAR2,
    p_payment_id OUT NUMBER) IS
    v_status bookings.status%TYPE; v_fare bookings.total_fare%TYPE;
    v_trip_status trips.status%TYPE; v_departure trips.departure_time%TYPE;
    v_active NUMBER; v_method VARCHAR2(40):=UPPER(TRIM(p_method));
  BEGIN
    IF v_method IS NULL OR v_method NOT IN ('SIMULATED','CASH','BANK_TRANSFER','CARD_SIMULATED') THEN
      RAISE_APPLICATION_ERROR(-20131,'Unsupported payment method');
    END IF;
    SELECT b.status,b.total_fare,t.status,t.departure_time
      INTO v_status,v_fare,v_trip_status,v_departure
      FROM bookings b JOIN trips t ON t.id=b.trip_id
      WHERE b.id=p_booking_id FOR UPDATE OF b.status;
    IF v_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20132,'Only pending bookings can receive a new payment');
    END IF;
    IF v_trip_status<>'SCHEDULED' OR v_departure<=LOCALTIMESTAMP THEN
      RAISE_APPLICATION_ERROR(-20133,'This booking is no longer payable');
    END IF;
    SELECT COUNT(*) INTO v_active FROM payments WHERE booking_id=p_booking_id
      AND status IN ('PENDING','COMPLETED');
    IF v_active>0 THEN
      RAISE_APPLICATION_ERROR(-20134,'An active or completed payment already exists for this booking');
    END IF;
    INSERT INTO payments(booking_id,amount,currency,payment_method,status,
      transaction_reference,notes,created_at)
      VALUES(p_booking_id,ROUND(v_fare,2),'LKR',v_method,'PENDING',
      'PAY-'||UPPER(RAWTOHEX(SYS_GUID())),p_notes,LOCALTIMESTAMP)
      RETURNING id INTO p_payment_id;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20103,'Booking not found');
  END;

  PROCEDURE delete_unsuccessful_payment(p_payment_id IN NUMBER) IS
    v_booking_id NUMBER; v_payment_status payments.status%TYPE;
    v_booking_status bookings.status%TYPE;
  BEGIN
    SELECT booking_id INTO v_booking_id FROM payments WHERE id=p_payment_id;
    SELECT status INTO v_booking_status FROM bookings WHERE id=v_booking_id FOR UPDATE;
    SELECT status INTO v_payment_status FROM payments WHERE id=p_payment_id FOR UPDATE;
    IF v_payment_status NOT IN ('PENDING','FAILED') THEN
      RAISE_APPLICATION_ERROR(-20135,'Completed or refunded payments must be retained for financial history');
    END IF;
    IF v_booking_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20136,'Only payments for pending bookings can be deleted');
    END IF;
    DELETE FROM payments WHERE id=p_payment_id;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20110,'Payment or booking not found');
  END;

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
    IF v_departure<=LOCALTIMESTAMP THEN
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
    IF v_status='COMPLETED' THEN RETURN; END IF;
    IF v_status<>'PENDING' OR v_booking_status<>'PENDING' THEN
      RAISE_APPLICATION_ERROR(-20107,'Only pending bookings and payments can be completed');
    END IF;
    IF v_amount<>v_fare THEN
      RAISE_APPLICATION_ERROR(-20108,'Payment amount does not match booking fare');
    END IF;
    IF v_trip_status<>'SCHEDULED' OR v_departure<=LOCALTIMESTAMP THEN
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
