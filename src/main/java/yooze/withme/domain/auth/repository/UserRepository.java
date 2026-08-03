package yooze.withme.domain.auth.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자 행을 비관 잠금으로 조회한다.
     * 카테고리 정렬 순서처럼 "사용자별 목록 전체를 읽고 계산해서 다시 쓰는" 작업은
     * 아직 존재하지 않는 행(신규 카테고리)까지 포함하므로 대상 행을 잠글 수 없다.
     * 대신 사용자 행 하나를 게이트로 삼아 같은 사용자의 요청을 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
