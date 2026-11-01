package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.common.base.BaseEntity;

@Entity
@Table(name = "user_withdrawals", indexes = @Index(name = "idx_user_withdrawals_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserWithdrawal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long withdrawalId;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 100)
    private String reason;

    @Column(length = 1000)
    private String opinion;
}
