package yooze.withme.domain.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.common.base.BaseEntity;

@Entity
@Table(name = "group_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupRound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "round_date", nullable = false)
    private LocalDate roundDate;

    @Column(name = "round_time", nullable = false)
    private LocalTime roundTime;

    @Column(name = "round_location_name", length = 127, nullable = false)
    private String roundLocationName;

    @Column(name = "round_location_address", nullable = false)
    private String roundLocationAddress;

    @Column(name = "round_location_prev_name", length = 127)
    private String roundLocationPrevName;

    @Column(name = "round_location_prev_address")
    private String roundLocationPrevAddress;

    @Column(name = "round_prev_date")
    private LocalDate roundPrevDate;

    @Column(name = "round_prev_time")
    private LocalTime roundPrevTime;

    @Column(name = "is_round_changed", nullable = false)
    private boolean roundChanged;

    /** 일정/장소 변경 시 이전 값을 prev 필드에 백업하고 새 값을 반영한다 */
    public void reschedule(LocalDate newDate, LocalTime newTime, String newLocationName, String newLocationAddress) {
        this.roundPrevDate = this.roundDate;
        this.roundPrevTime = this.roundTime;
        this.roundLocationPrevName = this.roundLocationName;
        this.roundLocationPrevAddress = this.roundLocationAddress;

        this.roundDate = newDate;
        this.roundTime = newTime;
        this.roundLocationName = newLocationName;
        this.roundLocationAddress = newLocationAddress;
        this.roundChanged = true;
    }
}
