package com.linkhub.analytics.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Composite ID class for ClickEvent entity.
 * Matches the partitioned table's composite PK (id, clicked_at).
 */
public class ClickEventId implements Serializable {

    private Long id;
    private Instant clickedAt;

    public ClickEventId() {}

    public ClickEventId(Long id, Instant clickedAt) {
        this.id = id;
        this.clickedAt = clickedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClickEventId that = (ClickEventId) o;
        return Objects.equals(id, that.id) && Objects.equals(clickedAt, that.clickedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clickedAt);
    }
}
