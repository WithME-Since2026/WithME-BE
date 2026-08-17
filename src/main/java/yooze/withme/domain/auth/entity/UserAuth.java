package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.domain.auth.enums.ProviderType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_auth")
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProviderType provider = ProviderType.LOCAL;

    @Column
    private Long providerUserId;

    /** LOCAL 로그인 전용. 카카오 사용자는 null */
    @Column(name = "login_id", length = 255)
    private String localId;

    /** LOCAL 로그인 전용. 카카오 사용자는 null */
    @Column(length = 255)
    private String password;

    /** 비밀번호 변경 */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
