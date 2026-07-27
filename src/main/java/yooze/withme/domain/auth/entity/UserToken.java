package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.enums.TokenStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "user_token",
        indexes = @Index(
                name = "idx_user_token_user_id_provider",
                columnList = "user_id, provider"
                // NOTE: DB 레벨에서 ACTIVE 상태에 대한 partial unique index 추가 필요
                // CREATE UNIQUE INDEX uq_user_token_active ON user_token (user_id, provider) WHERE status = 'ACTIVE'
        )
)
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 카카오 리프레시 토큰 — SHA-256 해시값으로 저장 */
    @Column(length = 64)
    private String kakaoRefreshHash;

    /** WithMe 리프레시 토큰 — SHA-256 해시값으로 저장 */
    @Column(length = 64)
    private String withmeRefreshHash;

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

    /** WithMe 리프레시 토큰 갱신 (SHA-256 해시로 저장) */
    public void updateWithmeRefresh(String withmeRefresh, LocalDateTime expiredAt) {
        this.withmeRefreshHash = sha256(withmeRefresh);
        this.expiredAt = expiredAt;
        this.status = TokenStatus.ACTIVE;
    }

    /** 토큰 비활성화 */
    public void deactivate() {
        this.status = TokenStatus.DELETED;
    }

    /** 토큰 해시 일치 여부 확인 */
    public boolean matchesWithmeRefresh(String rawToken) {
        return sha256(rawToken).equals(this.withmeRefreshHash);
    }

    private static String sha256(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
