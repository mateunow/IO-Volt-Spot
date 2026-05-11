package pl.voltspot.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "station_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @PrePersist
    protected void prePersist() {
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
