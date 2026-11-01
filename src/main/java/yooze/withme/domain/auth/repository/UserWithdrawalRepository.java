package yooze.withme.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.UserWithdrawal;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {}
