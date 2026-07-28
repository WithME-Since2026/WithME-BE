package yooze.withme.domain.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.domain.todo.enums.RepetitionDay;
import yooze.withme.domain.todo.enums.RepetitionEndType;
import yooze.withme.domain.todo.enums.RepetitionType;

/**
 * 할 일의 반복 설정. 하나의 할 일에 최대 하나의 반복 설정만 존재하므로 todo_id 에 유니크 제약을 둔다.
 * (ERD 의 복합 PK(id, todo_id) 는 대리키 id 만으로 이미 유일하므로 단일 PK + todo_id 유니크로 대체)
 */
@Entity
@Table(name = "todo_repetitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TodoRepetition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false, unique = true)
    private Todo todo;

    @Enumerated(EnumType.STRING)
    @Column(name = "repetition_type", nullable = false, length = 20)
    @Builder.Default
    private RepetitionType repetitionType = RepetitionType.NONE;

    /** 반복 간격 (예: 2 + WEEK = 2주마다) */
    @Column(name = "repetition_num", nullable = false)
    @Builder.Default
    private Long repetitionNum = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "repetition_day", nullable = false, length = 10)
    @Builder.Default
    private RepetitionDay repetitionDay = RepetitionDay.MON;

    @Enumerated(EnumType.STRING)
    @Column(name = "repetition_end_type", nullable = false, length = 20)
    @Builder.Default
    private RepetitionEndType repetitionEndType = RepetitionEndType.NONE;

    @Column(name = "repetition_end_date")
    private LocalDate repetitionEndDate;

    @Column(name = "repetition_end_count")
    private Long repetitionEndCount;

    /** 반복 설정 전체 교체 — 종료 조건이 바뀌면 사용하지 않는 종료 값은 비운다 */
    public void update(RepetitionType repetitionType, Long repetitionNum, RepetitionDay repetitionDay,
                       RepetitionEndType repetitionEndType, LocalDate repetitionEndDate, Long repetitionEndCount) {
        this.repetitionType = repetitionType;
        this.repetitionNum = repetitionNum;
        this.repetitionDay = repetitionDay;
        this.repetitionEndType = repetitionEndType;

        if (repetitionEndType == RepetitionEndType.DATE) {
            this.repetitionEndDate = repetitionEndDate;
            this.repetitionEndCount = null;
        } else if (repetitionEndType == RepetitionEndType.COUNT) {
            this.repetitionEndDate = null;
            this.repetitionEndCount = repetitionEndCount;
        } else {
            this.repetitionEndDate = null;
            this.repetitionEndCount = null;
        }
    }
}