package com.smartmove.backend.service;

import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Executes the Oracle booking/payment package on Spring's transaction-bound connection.
 * No COMMIT/ROLLBACK in the package: Spring owns transaction boundaries.
 */
@Component
public class BookingPaymentPlsqlGateway {
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public BookingPaymentPlsqlGateway(JdbcTemplate jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    private interface StatementBinder { void bind(CallableStatement call) throws SQLException; }

    private <T> T execute(String sql, StatementBinder binder, int outIndex, Class<T> resultType) {
        entityManager.flush();
        try {
            T result = jdbc.execute((java.sql.Connection connection) -> {
                try (CallableStatement call = connection.prepareCall(sql)) {
                    binder.bind(call);
                    if (outIndex > 0) call.registerOutParameter(outIndex, Types.NUMERIC);
                    call.execute();
                    if (outIndex == 0) return null;
                    Number value = (Number) call.getObject(outIndex);
                    return resultType.cast(outIndex > 0 && resultType == Long.class
                        ? Long.valueOf(value.longValue()) : value);
                }
            });
            entityManager.clear(); // prevent stale JPA objects after PL/SQL DML
            return result;
        } catch (org.springframework.dao.DataAccessException ex) {
            Throwable cause = ex;
            while (cause != null && !(cause instanceof SQLException)) cause = cause.getCause();
            if (cause instanceof SQLException sqlError) {
                int code = sqlError.getErrorCode();
                if (code >= 20101 && code <= 20136) {
                    String msg = sqlError.getMessage();
                    int begin = msg.indexOf("ORA-");
                    if (begin >= 0) msg = msg.substring(begin).split("\\n")[0];
                    HttpStatus status = switch (code) {
                        case 20103, 20110, 20123, 20128, 20130 -> HttpStatus.NOT_FOUND;
                        case 20112, 20114, 20120, 20121, 20122, 20124, 20131 -> HttpStatus.BAD_REQUEST;
                        default -> HttpStatus.CONFLICT;
                    };
                    throw new ResponseStatusException(status, msg, ex);
                }
            }
            throw ex;
        }
    }

    public Long createBooking(Long passenger, Long trip, Long board, Long dest, int seats) {
        return execute("{call sm_booking_payments.create_booking(?,?,?,?,?,?)}", c -> {
            c.setLong(1,passenger); c.setLong(2,trip); c.setLong(3,board);
            c.setLong(4,dest); c.setInt(5,seats);
        },6,Long.class);
    }
    public Long createPayment(Long booking, String method, String notes) {
        return execute("{call sm_booking_payments.create_payment(?,?,?,?)}", c -> {
            c.setLong(1,booking); c.setString(2,method); c.setString(3,notes);
        },4,Long.class);
    }
    public void confirmBooking(Long id) {
        execute("{call sm_booking_payments.confirm_booking(?)}",c->c.setLong(1,id),0,Long.class);
    }
    public void cancelBooking(Long id) {
        execute("{call sm_booking_payments.cancel_unpaid_booking(?)}",c->c.setLong(1,id),0,Long.class);
    }
    public void completePayment(Long id) {
        execute("{call sm_booking_payments.complete_payment(?)}",c->c.setLong(1,id),0,Long.class);
    }
    public void failPayment(Long id,String reason) {
        execute("{call sm_booking_payments.fail_payment(?,?)}",c->{
            c.setLong(1,id);c.setString(2,reason);
        },0,Long.class);
    }
    public void refundPayment(Long id,BigDecimal amount,String reason) {
        execute("{call sm_booking_payments.refund_payment(?,?,?)}",c->{
            c.setLong(1,id);c.setBigDecimal(2,amount);c.setString(3,reason);
        },0,Long.class);
    }
    public void deletePayment(Long id) {
        execute("{call sm_booking_payments.delete_unsuccessful_payment(?)}",c->c.setLong(1,id),0,Long.class);
    }
    public int maximumOccupied(Long trip,int boardOrder,int destOrder) {
        Number n = jdbc.queryForObject(
            "SELECT sm_booking_payments.max_occupied(?,?,?) FROM dual",
            Number.class,trip,boardOrder,destOrder);
        return n == null ? 0 : n.intValue();
    }
    public BigDecimal segmentFare(Long trip,Long board,Long dest) {
        return jdbc.queryForObject(
            "SELECT sm_booking_payments.segment_fare(?,?,?) FROM dual",
            BigDecimal.class,trip,board,dest);
    }
}
