package pl.voltspot.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.voltspot.backend.enums.OverrideState;
import pl.voltspot.backend.enums.ReportedStatus;

import java.time.Instant;

@Entity
@Table(name = "community_status_overrides")
@Getter
@Setter
@NoArgsConstructor
public class CommunityStatusOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "reported_status", nullable = false, length = 20)
    private ReportedStatus reportedStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverrideState state = OverrideState.PENDING;

    @Column(name = "working_count", nullable = false)
    private int workingCount = 0;

    @Column(name = "not_working_count", nullable = false)
    private int notWorkingCount = 0;

    @Column(name = "consecutive_count", nullable = false)
    private int consecutiveCount = 0;

    @Column(name = "challenge_count", nullable = false)
    private int challengeCount = 0;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_admin_id")
    private User confirmedByAdmin;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
