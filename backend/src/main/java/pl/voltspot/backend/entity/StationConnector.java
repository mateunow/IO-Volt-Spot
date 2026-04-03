package pl.voltspot.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "station_connectors")
@Getter
@Setter
@NoArgsConstructor
public class StationConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "connector_type", nullable = false, length = 100)
    private String connectorType;

    @Column(name = "current_type", length = 10)
    private String currentType;

    @Column(name = "power_kw", precision = 8, scale = 2)
    private BigDecimal powerKw;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "external_connector_key", length = 120)
    private String externalConnectorKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}