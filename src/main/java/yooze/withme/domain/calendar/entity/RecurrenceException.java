package yooze.withme.domain.calendar.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.domain.calendar.enums.RecurrenceExceptionType;

@Entity
@Table(
        name = "recurrence_exceptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recurrence_exceptions_occurrence",
                columnNames = {"recurrence_id", "occurrence_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecurrenceException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Long exceptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_id", nullable = false)
    private Recurrence recurrence;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 10)
    private RecurrenceExceptionType exceptionType;

    @Column(name = "override_date")
    private LocalDate overrideDate;

    @Column(name = "override_title", length = 255)
    private String overrideTitle;

    @Column(name = "override_start_time")
    private LocalTime overrideStartTime;

    @Column(name = "override_end_time")
    private LocalTime overrideEndTime;

    @Column(name = "override_completed")
    private Boolean overrideCompleted;

    /** 이 회차를 건너뛴다. 덮어쓰기 값은 모두 버린다. */
    public void skip() {
        this.exceptionType = RecurrenceExceptionType.SKIP;
        this.overrideDate = null;
        this.overrideTitle = null;
        this.overrideStartTime = null;
        this.overrideEndTime = null;
        this.overrideCompleted = null;
    }

    /**
     * 이 회차만 덮어쓴다. null인 필드는 이번 요청에 없다는 뜻이므로 기존 덮어쓰기 값을 유지한다.
     * 덮어쓴 적이 없는 필드는 계속 null로 남아 원본(일정/Todo) 값을 그대로 쓴다.
     */
    public void override(
            LocalDate overrideDate,
            String overrideTitle,
            LocalTime overrideStartTime,
            LocalTime overrideEndTime,
            Boolean overrideCompleted
    ) {
        this.exceptionType = RecurrenceExceptionType.OVERRIDE;
        if (overrideDate != null) {
            this.overrideDate = overrideDate;
        }
        if (overrideTitle != null) {
            this.overrideTitle = overrideTitle;
        }
        if (overrideStartTime != null) {
            this.overrideStartTime = overrideStartTime;
        }
        if (overrideEndTime != null) {
            this.overrideEndTime = overrideEndTime;
        }
        if (overrideCompleted != null) {
            this.overrideCompleted = overrideCompleted;
        }
    }
}
