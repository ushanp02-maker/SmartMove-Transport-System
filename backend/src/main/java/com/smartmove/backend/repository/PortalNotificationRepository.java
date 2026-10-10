package com.smartmove.backend.repository;

import com.smartmove.backend.model.PortalNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PortalNotificationRepository extends MongoRepository<PortalNotification,String> {
    List<PortalNotification> findTop100ByRecipientAccountIdOrderByCreatedAtDesc(Long recipientAccountId);
    Optional<PortalNotification> findByIdAndRecipientAccountId(String id,Long recipientAccountId);
    long countByRecipientAccountIdAndReadFalse(Long recipientAccountId);
}
