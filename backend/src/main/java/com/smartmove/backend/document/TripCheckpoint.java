package com.smartmove.backend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "trip_checkpoints")
public class TripCheckpoint {
    @Id private String id;
    private Long tripId;
    private Long stopId;
    private Integer stopOrder;
    private String stopName;
    private Long driverId;
    private LocalDateTime confirmedAt;
    public String getId(){return id;}
    public void setId(String v){id=v;}
    public Long getTripId(){return tripId;}
    public void setTripId(Long v){tripId=v;}
    public Long getStopId(){return stopId;}
    public void setStopId(Long v){stopId=v;}
    public Integer getStopOrder(){return stopOrder;}
    public void setStopOrder(Integer v){stopOrder=v;}
    public String getStopName(){return stopName;}
    public void setStopName(String v){stopName=v;}
    public Long getDriverId(){return driverId;}
    public void setDriverId(Long v){driverId=v;}
    public LocalDateTime getConfirmedAt(){return confirmedAt;}
    public void setConfirmedAt(LocalDateTime v){confirmedAt=v;}
}
