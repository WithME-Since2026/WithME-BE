package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.enums.TokenStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_token")
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 512)
    private String kakaoRefresh;

    @Column(length = 512)
    private String withmeRefresh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProviderType provider = ProviderType.LOCAL;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;

    /** WithMe 리프레시 토큰 갱신 */
    public void updateWithmeRefresh(String withmeRefresh, LocalDateTime expiredAt) {
        this.withmeRefresh = withmeRefresh;
        this.expiredAt = expiredAt;
        this.status = TokenStatus.ACTIVE;
    }

    /** 토큰 비활성화 */
    public void deactivate() {
        this.status = TokenStatus.DELETED;
    }
}
