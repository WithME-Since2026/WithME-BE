package yooze.withme.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
