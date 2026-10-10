package com.smartmove.backend.service;

import com.smartmove.backend.model.PortalNotification;
import com.smartmove.backend.repository.PortalNotificationRepository;
import com.smartmove.backend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PortalNotificationService {
    private final PortalNotificationRepository notifications;
    private final UserAccountRepository accounts;
    public PortalNotificationService(PortalNotificationRepository notifications,UserAccountRepository accounts) {
        this.notifications=notifications;this.accounts=accounts;
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
