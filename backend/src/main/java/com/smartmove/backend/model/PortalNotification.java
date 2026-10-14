package com.smartmove.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "portal_notifications")
public class PortalNotification {
    @Id public String id;
    public Long recipientAccountId;
    public String title;
    public String message;
    public String category;
    public String link;
    public boolean read;
    public LocalDateTime createdAt = LocalDateTime.now();

    public PortalNotification(Long recipientAccountId,String title,String message,String category,String link) {
        this.recipientAccountId=recipientAccountId;
        this.title=title;
        this.message=message;
        this.category=category;
        this.link=link;
    }
}
