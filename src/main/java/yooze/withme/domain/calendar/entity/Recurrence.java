package yooze.withme.domain.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;

@Entity
@Table(
        name = "recurrences",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recurrences_owner",
                columnNames = {"owner_type", "owner_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Recurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_id")
    private Long recurrenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private RecurrenceOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFreq freq;

    @Column(name = "repeat_interval", nullable = false)
    @Builder.Default
    private int repeatInterval = 1;

    @Column(name = "by_days", length = 27)
    private String byDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_type", nullable = false, length = 10)
    private RecurrenceEndType endType;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_count")
    private Integer endCount;

    public void update(
            RecurrenceFreq freq,
            int repeatInterval,
            String byDays,
            RecurrenceEndType endType,
            LocalDate endDate,
            Integer endCount
    ) {
        this.freq = freq;
        this.repeatInterval = repeatInterval;
        this.byDays = byDays;
        this.endType = endType;
        this.endDate = endType == RecurrenceEndType.DATE ? endDate : null;
        this.endCount = endType == RecurrenceEndType.COUNT ? endCount : null;
    }
}
