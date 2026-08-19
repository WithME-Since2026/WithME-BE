package yooze.withme.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.notification.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 유저의 알림 목록 조회 (최신순) */
    List<Notification> findAllByUserUserIdOrderByCreatedAtDesc(Long userId);

    /** 유저의 안읽은 알림 수 */
    long countByUserUserIdAndReadAtIsNull(Long userId);

    /** 유저의 전체 알림 읽음 처리 */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = now() WHERE n.user.userId = :userId AND n.readAt IS NULL")
    void markAllAsRead(@Param("userId") Long userId);
}
