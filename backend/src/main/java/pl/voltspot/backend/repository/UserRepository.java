package pl.voltspot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.voltspot.backend.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}