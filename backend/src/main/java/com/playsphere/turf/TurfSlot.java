package com.playsphere.turf;
import com.playsphere.common.Ids;import jakarta.persistence.*;import java.math.BigDecimal;import java.time.Instant;
@Entity @Table(name="turf_slots") public class TurfSlot{
 @Id @Column(length=36) private String id;@Column(name="turf_id",nullable=false,length=36) private String turfId;@Column(name="start_at",nullable=false) private Instant startAt;@Column(name="end_at",nullable=false) private Instant endAt;@Column(nullable=false,precision=12,scale=2) private BigDecimal price;@Column(nullable=false,length=20) private String status;@Column(name="created_at",nullable=false) private Instant createdAt;
 protected TurfSlot(){}public TurfSlot(String turf,Instant start,Instant end,BigDecimal price){id=Ids.uuid();turfId=turf;startAt=start;endAt=end;this.price=price;status="AVAILABLE";createdAt=Instant.now();}
 public String getId(){return id;}public String getTurfId(){return turfId;}public Instant getStartAt(){return startAt;}public Instant getEndAt(){return endAt;}public BigDecimal getPrice(){return price;}public String getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}public void book(){status="BOOKED";}public void release(){status="AVAILABLE";}public void block(){status="BLOCKED";}public void makeAvailable(){status="AVAILABLE";}
}
