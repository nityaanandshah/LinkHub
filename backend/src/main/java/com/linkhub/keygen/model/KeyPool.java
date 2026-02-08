package com.linkhub.keygen.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "key_pool")
public class KeyPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_key", unique = true, nullable = false, length = 7)
    private String shortKey;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    // Constructors
    public KeyPool() {}

    public KeyPool(String shortKey) {
        this.shortKey = shortKey;
        this.isUsed = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortKey() { return shortKey; }
    public void setShortKey(String shortKey) { this.shortKey = shortKey; }

    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }

    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
}
