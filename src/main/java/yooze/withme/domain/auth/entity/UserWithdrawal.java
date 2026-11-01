package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.common.base.BaseEntity;

@Entity
@Table(name = "user_withdrawals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserWithdrawal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long withdrawalId;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 1000)
    private String opinion;
}
