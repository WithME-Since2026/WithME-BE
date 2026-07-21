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

    @Column(name = "login_id", nullable = false, length = 255)
    private String localId;

    @Column(nullable = false, length = 255)
    private String password;

    /** 비밀번호 변경 */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
