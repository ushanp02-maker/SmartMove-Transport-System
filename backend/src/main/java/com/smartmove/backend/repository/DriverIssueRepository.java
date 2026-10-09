package com.smartmove.backend.repository;
import com.smartmove.backend.entity.DriverIssue;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface DriverIssueRepository extends MongoRepository<DriverIssue,String> {
    List<DriverIssue> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<DriverIssue> findAllByOrderByCreatedAtDesc();
}
