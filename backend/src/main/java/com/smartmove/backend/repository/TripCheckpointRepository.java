package com.smartmove.backend.repository;

import com.smartmove.backend.document.TripCheckpoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TripCheckpointRepository extends MongoRepository<TripCheckpoint,String> {
    List<TripCheckpoint> findByTripIdOrderByStopOrderAsc(Long tripId);
}
