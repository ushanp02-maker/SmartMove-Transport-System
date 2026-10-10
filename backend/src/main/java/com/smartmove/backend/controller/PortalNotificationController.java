package com.smartmove.backend.controller;

import com.smartmove.backend.model.PortalNotification;
import com.smartmove.backend.repository.PortalNotificationRepository;
import com.smartmove.backend.service.CurrentUserService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class PortalNotificationController {
    private final PortalNotificationRepository notifications;
    private final CurrentUserService users;
    public PortalNotificationController(PortalNotificationRepository notifications,CurrentUserService users) {
        this.notifications=notifications;this.users=users;
    }
    @GetMapping
    public List<PortalNotification> mine() {
        return notifications.findTop100ByRecipientAccountIdOrderByCreatedAtDesc(users.getCurrentAccountId());
    }
    @GetMapping("/unread-count")
    public Map<String,Long> unreadCount() {
        return Map.of("count",notifications.countByRecipientAccountIdAndReadFalse(users.getCurrentAccountId()));
    }
    @PatchMapping("/{id}/read")
    public PortalNotification read(@PathVariable String id) {
        var item=notifications.findByIdAndRecipientAccountId(id,users.getCurrentAccountId())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));
        item.read=true;
        return notifications.save(item);
    }
    @PatchMapping("/read-all")
    public Map<String,Integer> readAll() {
        var items=notifications.findTop100ByRecipientAccountIdOrderByCreatedAtDesc(users.getCurrentAccountId());
        var unread=items.stream().filter(item->!item.read).toList();
        unread.forEach(item->item.read=true);
        notifications.saveAll(unread);
        return Map.of("updated",unread.size());
    }
}
