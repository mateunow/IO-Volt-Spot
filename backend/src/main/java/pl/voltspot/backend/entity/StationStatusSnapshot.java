package pl.voltspot.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "station_status_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class StationStatusSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "available_count", nullable = false)
    private Integer availableCount = 0;

    @Column(name = "occupied_count", nullable = false)
    private Integer occupiedCount = 0;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount = 0;

    @Column(name = "out_of_service_count", nullable = false)
    private Integer outOfServiceCount = 0;

    @Column(name = "unknown_count", nullable = false)
    private Integer unknownCount = 0;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}