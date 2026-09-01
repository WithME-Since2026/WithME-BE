package yooze.withme.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.common.base.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 40)
    private String nickname;

    @Column(length = 1024)
    private String profileImg;

    @Column
    private LocalDateTime deletedAt;

    @Column
    private Long kakaoId;

    @Column(nullable = false)
    private boolean kakaoSync;

    @Column(nullable = false)
    private boolean notifyGroupRemind;

    @Column(nullable = false)
    private boolean notifyTodoDeadline;

    @Column(nullable = false)
    private boolean notifyGroupInvite;

    /** 소프트 삭제 */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 닉네임 변경 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 알림 설정 변경 */
    public void updateNotificationSettings(boolean notifyGroupRemind, boolean notifyTodoDeadline, boolean notifyGroupInvite) {
        this.notifyGroupRemind = notifyGroupRemind;
        this.notifyTodoDeadline = notifyTodoDeadline;
        this.notifyGroupInvite = notifyGroupInvite;
    }
}
