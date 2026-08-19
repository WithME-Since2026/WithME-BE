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
        uniqueConstraints = @UniqueConstraint(name = "uq_fcm_tokens_user", columnNames = "user_id"))
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    public void updateToken(String token) {
        this.token = token;
    }
}
