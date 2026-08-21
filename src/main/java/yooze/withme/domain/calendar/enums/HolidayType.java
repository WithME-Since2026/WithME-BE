package yooze.withme.domain.calendar.enums;

/** 공공데이터포털 특일 정보의 오퍼레이션별 구분. */
public enum HolidayType {
    /** getRestDeInfo — 공휴일 */
    HOLIDAY,
    /** getHoliDeInfo — 국경일 */
    NATIONAL,
    /** getAnniversaryInfo — 기념일 */
    ANNIVERSARY,
    /** get24DivisionsInfo — 24절기 */
    SOLAR_TERM,
    /** getSundryDayInfo — 잡절 */
    SUNDRY
}
