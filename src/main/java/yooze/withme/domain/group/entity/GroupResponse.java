package yooze.withme.domain.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.domain.group.enums.AttendanceStatus;

@Entity
@Table(name = "group_response")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private GroupRound groupRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private GroupMember member;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Column(name = "absence_reason")
    private String absenceReason;

    /** 회차 일정이 변경됐을 때 재확인 상태로 전환 */
    public void markRerespond() {
        this.attendanceStatus = AttendanceStatus.RERESPONSE;
    }

    /** 참여자가 본인 출석 여부를 응답 - ATTEND로 바뀌면 기존 결석 사유는 제거한다 */
    public void respond(AttendanceStatus status, String reason) {
        this.attendanceStatus = status;
        if (status == AttendanceStatus.ATTEND) {
            this.absenceReason = null;
        } else {
            this.absenceReason = reason;
        }
    }
}
