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
import yooze.withme.domain.calendar.enums.HolidayType;

/**
 * 공공데이터포털에서 받아 캐싱한 특일. 캘린더 조회는 이 테이블만 본다.
 * 같은 날짜에 여러 특일이 겹칠 수 있어(현충일 + 절기) 날짜를 PK로 두지 않는다.
 */
@Entity
@Table(
        name = "holidays",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_holidays_date_name",
                columnNames = {"date", "name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HolidayType type;

    /** 실제로 쉬는 날(빨간날)인지. getRestDeInfo 의 isHoliday=Y 기준. */
    @Column(name = "rest_day", nullable = false)
    private boolean restDay;

    public void update(HolidayType type, boolean restDay) {
        this.type = type;
        this.restDay = restDay;
    }
}
