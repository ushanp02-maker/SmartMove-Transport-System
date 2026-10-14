-- SmartMove Oracle PL/SQL: run as the application's schema owner in SQL Developer (Run Script/F5).
-- This script does not delete or reset existing application data.
CREATE OR REPLACE PACKAGE sm_reports AS
  FUNCTION occupied_seats(p_trip_id NUMBER, p_board_order NUMBER, p_dest_order NUMBER) RETURN NUMBER;
  PROCEDURE route_popularity(p_result OUT SYS_REFCURSOR);
  PROCEDURE revenue(p_start DATE, p_end DATE, p_result OUT SYS_REFCURSOR);
  PROCEDURE passenger_history(p_passenger_id NUMBER, p_result OUT SYS_REFCURSOR);
  PROCEDURE maintenance_due(p_until DATE, p_result OUT SYS_REFCURSOR);
  PROCEDURE driver_performance(p_result OUT SYS_REFCURSOR);
  PROCEDURE booking_summary(p_start DATE, p_end DATE, p_result OUT SYS_REFCURSOR);
  PROCEDURE fleet_utilization(p_result OUT SYS_REFCURSOR);
  PROCEDURE daily_revenue(p_start DATE, p_end DATE, p_result OUT SYS_REFCURSOR);
  PROCEDURE maintenance_cost(p_start DATE, p_end DATE, p_result OUT SYS_REFCURSOR);
END sm_reports;
/
CREATE OR REPLACE PACKAGE BODY sm_reports AS
  PROCEDURE check_dates(p_start DATE, p_end DATE) IS
  BEGIN
    IF p_start IS NULL OR p_end IS NULL OR TRUNC(p_start)>TRUNC(p_end) THEN
      RAISE_APPLICATION_ERROR(-20020,'Invalid report date range');
    END IF;
  END;
  FUNCTION occupied_seats(p_trip_id NUMBER, p_board_order NUMBER, p_dest_order NUMBER) RETURN NUMBER IS
    v_count NUMBER;
  BEGIN
    IF p_trip_id IS NULL OR p_board_order >= p_dest_order THEN
      RAISE_APPLICATION_ERROR(-20021,'Invalid trip or stop order');
    END IF;
    SELECT NVL(SUM(b.seat_count),0) INTO v_count FROM bookings b
      JOIN route_stops s1 ON s1.id=b.boarding_stop_id
      JOIN route_stops s2 ON s2.id=b.destination_stop_id
      WHERE b.trip_id=p_trip_id AND UPPER(b.status) IN ('PENDING','CONFIRMED')
      AND s1.stop_order < p_dest_order AND s2.stop_order > p_board_order;
    RETURN v_count;
  EXCEPTION
    WHEN VALUE_ERROR THEN RAISE_APPLICATION_ERROR(-20022,'Invalid numeric input for occupied seats');
  END;
  PROCEDURE route_popularity(p_result OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_result FOR SELECT r.id route_id,r.name route_name,r.origin,r.destination,
      COUNT(DISTINCT t.id) total_trips,COUNT(b.id) total_bookings,
      NVL(SUM(CASE WHEN b.status='CONFIRMED' THEN b.seat_count ELSE 0 END),0) confirmed_seats
      FROM routes r LEFT JOIN trips t ON t.route_id=r.id
      LEFT JOIN bookings b ON b.trip_id=t.id
      GROUP BY r.id,r.name,r.origin,r.destination ORDER BY total_bookings DESC,r.name;
  END;
  PROCEDURE revenue(p_start DATE,p_end DATE,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    check_dates(p_start,p_end);
    OPEN p_result FOR SELECT
      COUNT(CASE WHEN status IN ('COMPLETED','REFUNDED') AND payment_date>=TRUNC(p_start)
        AND payment_date<TRUNC(p_end)+1 THEN 1 END) successful_payments,
      COUNT(CASE WHEN refunded_at>=TRUNC(p_start) AND refunded_at<TRUNC(p_end)+1 THEN 1 END) refunded_payments,
      NVL(SUM(CASE WHEN status IN ('COMPLETED','REFUNDED') AND payment_date>=TRUNC(p_start)
        AND payment_date<TRUNC(p_end)+1 THEN amount ELSE 0 END),0) gross_revenue,
      NVL(SUM(CASE WHEN refunded_at>=TRUNC(p_start) AND refunded_at<TRUNC(p_end)+1
        THEN refund_amount ELSE 0 END),0) refunds_issued
      FROM payments;
  END;
  PROCEDURE passenger_history(p_passenger_id NUMBER,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    IF p_passenger_id IS NULL THEN RAISE_APPLICATION_ERROR(-20023,'Passenger ID required'); END IF;
    OPEN p_result FOR SELECT b.id booking_id,b.booking_reference,b.status booking_status,
      b.seat_count,b.total_fare,t.departure_time,t.arrival_time,r.name route_name,
      s1.stop_name boarding_stop,s2.stop_name destination_stop
      FROM bookings b JOIN trips t ON t.id=b.trip_id JOIN routes r ON r.id=t.route_id
      JOIN route_stops s1 ON s1.id=b.boarding_stop_id
      JOIN route_stops s2 ON s2.id=b.destination_stop_id
      WHERE b.passenger_id=p_passenger_id ORDER BY t.departure_time DESC;
  END;
  PROCEDURE maintenance_due(p_until DATE,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    IF p_until IS NULL THEN RAISE_APPLICATION_ERROR(-20024,'Due date required'); END IF;
    OPEN p_result FOR SELECT v.id vehicle_id,v.registration_number,v.name vehicle_name,
      v.next_service_date,m.id maintenance_id,m.scheduled_date,m.status maintenance_status
      FROM vehicles v LEFT JOIN maintenance m ON m.vehicle_id=v.id
        AND m.status IN ('SCHEDULED','IN_PROGRESS')
      WHERE v.next_service_date<=TRUNC(p_until)
        OR m.scheduled_date<=TRUNC(p_until)
      ORDER BY v.next_service_date,v.registration_number;
  END;
  PROCEDURE driver_performance(p_result OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_result FOR SELECT d.id driver_id,d.name driver_name,d.status,
      COUNT(t.id) assigned_trips,
      COUNT(CASE WHEN t.status='COMPLETED' THEN 1 END) completed_trips,
      COUNT(CASE WHEN t.status='CANCELLED' THEN 1 END) cancelled_trips,
      (SELECT COUNT(*) FROM bookings b JOIN trips t2 ON t2.id=b.trip_id WHERE t2.driver_id=d.id AND b.status='CONFIRMED') confirmed_bookings
      FROM drivers d LEFT JOIN trips t ON t.driver_id=d.id
      GROUP BY d.id,d.name,d.status ORDER BY completed_trips DESC,d.name;
  END;
  PROCEDURE booking_summary(p_start DATE,p_end DATE,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    check_dates(p_start,p_end);
    OPEN p_result FOR SELECT COUNT(*) total_bookings,
      COUNT(CASE WHEN status='PENDING' THEN 1 END) pending_bookings,
      COUNT(CASE WHEN status='CONFIRMED' THEN 1 END) confirmed_bookings,
      COUNT(CASE WHEN status='CANCELLED' THEN 1 END) cancelled_bookings,
      COUNT(CASE WHEN status='EXPIRED' THEN 1 END) expired_bookings,
      NVL(SUM(CASE WHEN status IN ('PENDING','CONFIRMED') THEN seat_count ELSE 0 END),0) total_seats_booked,
      NVL(SUM(CASE WHEN status IN ('PENDING','CONFIRMED') THEN total_fare ELSE 0 END),0) total_booking_value
      FROM bookings WHERE booked_at>=TRUNC(p_start) AND booked_at<TRUNC(p_end)+1;
  END;
  PROCEDURE fleet_utilization(p_result OUT SYS_REFCURSOR) IS
  BEGIN
    OPEN p_result FOR SELECT v.id vehicle_id,v.registration_number,v.name vehicle_name,v.status,
      v.seating_capacity,COUNT(t.id) assigned_trips,
      COUNT(CASE WHEN t.status='COMPLETED' THEN 1 END) completed_trips,
      COUNT(CASE WHEN t.status='CANCELLED' THEN 1 END) cancelled_trips
      FROM vehicles v LEFT JOIN trips t ON t.vehicle_id=v.id
      GROUP BY v.id,v.registration_number,v.name,v.status,v.seating_capacity
      ORDER BY assigned_trips DESC;
  END;
  PROCEDURE daily_revenue(p_start DATE,p_end DATE,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    check_dates(p_start,p_end);
    OPEN p_result FOR
      WITH days AS (SELECT TRUNC(p_start)+LEVEL-1 report_date FROM dual
                    CONNECT BY LEVEL <= TRUNC(p_end)-TRUNC(p_start)+1)
      SELECT d.report_date,
      (SELECT COUNT(*) FROM payments p WHERE TRUNC(p.payment_date)=d.report_date
        AND p.status IN ('COMPLETED','REFUNDED')) successful_payments,
      (SELECT NVL(SUM(p.amount),0) FROM payments p WHERE TRUNC(p.payment_date)=d.report_date
        AND p.status IN ('COMPLETED','REFUNDED')) gross_revenue,
      (SELECT NVL(SUM(p.refund_amount),0) FROM payments p WHERE TRUNC(p.refunded_at)=d.report_date) refunds
      FROM days d ORDER BY d.report_date;
  END;
  PROCEDURE maintenance_cost(p_start DATE,p_end DATE,p_result OUT SYS_REFCURSOR) IS
  BEGIN
    check_dates(p_start,p_end);
    OPEN p_result FOR SELECT COUNT(*) completed_maintenance_records,
      NVL(SUM(actual_cost),0) total_actual_cost,
      NVL(SUM(estimated_cost),0) total_estimated_cost
      FROM maintenance WHERE status='COMPLETED'
      AND completed_date>=TRUNC(p_start) AND completed_date<TRUNC(p_end)+1;
  END;
END sm_reports;
/
-- Demonstration cursor with explicit OPEN/FETCH/CLOSE and exception handling.
CREATE OR REPLACE PROCEDURE sm_top_routes_demo IS
  v_cursor SYS_REFCURSOR;
  v_id NUMBER; v_name VARCHAR2(150); v_origin VARCHAR2(120);
  v_destination VARCHAR2(120); v_trips NUMBER; v_bookings NUMBER; v_seats NUMBER;
BEGIN
  sm_reports.route_popularity(v_cursor);
  LOOP
    FETCH v_cursor INTO v_id,v_name,v_origin,v_destination,v_trips,v_bookings,v_seats;
    EXIT WHEN v_cursor%NOTFOUND;
    DBMS_OUTPUT.PUT_LINE(v_name||' : '||v_bookings||' bookings');
  END LOOP;
  CLOSE v_cursor;
EXCEPTION
  WHEN OTHERS THEN
    IF v_cursor%ISOPEN THEN CLOSE v_cursor; END IF;
    RAISE;
END;
/
-- Business rule: invalid route-stop ordering is rejected by Oracle, not only by Java.
CREATE OR REPLACE TRIGGER sm_booking_stop_guard
BEFORE INSERT OR UPDATE OF boarding_stop_id,destination_stop_id,trip_id ON bookings
FOR EACH ROW
DECLARE
  v_board_route NUMBER; v_dest_route NUMBER; v_trip_route NUMBER;
  v_board_order NUMBER; v_dest_order NUMBER;
BEGIN
  SELECT route_id,stop_order INTO v_board_route,v_board_order FROM route_stops WHERE id=:NEW.boarding_stop_id;
  SELECT route_id,stop_order INTO v_dest_route,v_dest_order FROM route_stops WHERE id=:NEW.destination_stop_id;
  SELECT route_id INTO v_trip_route FROM trips WHERE id=:NEW.trip_id;
  IF v_board_route<>v_trip_route OR v_dest_route<>v_trip_route OR v_board_order>=v_dest_order THEN
    RAISE_APPLICATION_ERROR(-20030,'Boarding and destination must be ordered stops on the trip route');
  END IF;
EXCEPTION
  WHEN NO_DATA_FOUND THEN RAISE_APPLICATION_ERROR(-20031,'Trip or route stop does not exist');
END;
/
