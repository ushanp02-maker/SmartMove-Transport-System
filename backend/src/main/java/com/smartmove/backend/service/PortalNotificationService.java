package com.smartmove.backend.service;

import com.smartmove.backend.model.PortalNotification;
import com.smartmove.backend.repository.PortalNotificationRepository;
import com.smartmove.backend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class PortalNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PortalNotificationService.class);
    private final PortalNotificationRepository notifications;
    private final UserAccountRepository accounts;
    public PortalNotificationService(PortalNotificationRepository notifications,UserAccountRepository accounts) {
        this.notifications=notifications;this.accounts=accounts;
    }
    // Deliver after Oracle commits, so rolled-back business actions do not produce alerts.
    // MongoDB delivery failures must not undo a successful Oracle operation.
    private void deliver(Runnable task) {
        Runnable safe = () -> { try { task.run(); } catch (RuntimeException ex) {
            log.error("MongoDB notification delivery failed", ex);
        }};
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { safe.run(); }
            });
        } else { safe.run(); }
    }

    public void notifyAccount(Long accountId, String title, String message, String category, String link) {
        if (accountId == null) return;
        deliver(() -> notifications.save(new PortalNotification(accountId, title, message, category, link)));
    }

    public void notifyPassenger(Long passengerId, String title, String message, String category, String link) {
        if (passengerId == null) return;
        deliver(() -> accounts.findByPassengerId(passengerId).ifPresent(account -> {
            if ("ACTIVE".equalsIgnoreCase(account.getAccountStatus()))
                notifications.save(new PortalNotification(account.getId(), title, message, category, link));
        }));
    }

    public void notifyDriver(Long driverId, String title, String message, String category, String link) {
        if (driverId == null) return;
        deliver(() -> accounts.findByDriverId(driverId).ifPresent(account -> {
            if ("ACTIVE".equalsIgnoreCase(account.getAccountStatus()))
                notifications.save(new PortalNotification(account.getId(), title, message, category, link));
        }));
    }

    public void notifyAdmins(String title, String message, String category, String link) {
        deliver(() -> {
            var recipients = new ArrayList<com.smartmove.backend.entity.UserAccount>();
            recipients.addAll(accounts.findByRoleAndAccountStatus("ADMIN", "ACTIVE"));
            recipients.addAll(accounts.findByRoleAndAccountStatus("SUPER_ADMIN", "ACTIVE"));
            notifications.saveAll(recipients.stream()
                .map(account -> new PortalNotification(account.getId(), title, message, category, link)).toList());
        });
    }

    public void notifyAudience(String audience,String title,String message,String link) {
        String target=String.valueOf(audience).toUpperCase(Locale.ROOT);
        List<String> roles=switch(target) {
            case "PASSENGER" -> List.of("PASSENGER");
            case "DRIVER" -> List.of("DRIVER");
            case "ADMIN" -> List.of("ADMIN","SUPER_ADMIN");
            default -> List.of("PASSENGER","DRIVER","ADMIN","SUPER_ADMIN");
        };
        var recipients=roles.stream().flatMap(role->accounts.findByRoleAndAccountStatus(role,"ACTIVE").stream()).toList();
        notifications.saveAll(recipients.stream().map(account->new PortalNotification(
            account.getId(),title,message,"ANNOUNCEMENT",link)).toList());
    }
}
