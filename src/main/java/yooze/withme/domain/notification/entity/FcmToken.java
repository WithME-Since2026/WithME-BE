package yooze.withme.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.common.base.BaseEntity;
import yooze.withme.domain.auth.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "fcm_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uq_fcm_tokens_user_device", columnNames = {"user_id", "device_id"}))
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 기기 고유 식별자 — 프론트에서 생성해서 전달 (같은 기기면 같은 값) */
    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(nullable = false, length = 512)
    private String token;

    public void updateToken(String token) {
        this.token = token;
    }
}
