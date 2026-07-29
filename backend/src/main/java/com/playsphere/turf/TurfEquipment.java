package com.playsphere.turf;

import com.playsphere.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "turf_equipment")
public class TurfEquipment {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "turf_id", nullable = false, length = 36)
    private String turfId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_per_booking", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerBooking;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TurfEquipment() {}

    public TurfEquipment(
            String turfId,
            String name,
            String description,
            int quantity,
            BigDecimal pricePerBooking
    ) {
        this.id = Ids.uuid();
        this.turfId = turfId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.pricePerBooking = pricePerBooking;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getTurfId() { return turfId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPricePerBooking() { return pricePerBooking; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(int quantity, BigDecimal pricePerBooking, boolean active) {
        this.quantity = quantity;
        this.pricePerBooking = pricePerBooking;
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
