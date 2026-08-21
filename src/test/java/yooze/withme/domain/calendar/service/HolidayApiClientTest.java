package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.enums.HolidayType;

class HolidayApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesMultipleItemsAndRestDayFlag() {
        List<Holiday> holidays = HolidayApiClient.parse(json("""
                {"response":{"body":{"items":{"item":[
                  {"locdate":20260815,"dateName":"광복절","isHoliday":"Y"},
                  {"locdate":20260101,"dateName":"1월1일","isHoliday":"Y"}
                ]}}}}
                """), HolidayType.HOLIDAY);

        assertThat(holidays).extracting(Holiday::getDate)
                .containsExactly(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 1, 1));
        assertThat(holidays).allMatch(Holiday::isRestDay);
    }

    @Test
    void parsesSingleItemReturnedAsObject() {
        List<Holiday> holidays = HolidayApiClient.parse(json("""
                {"response":{"body":{"items":{"item":
                  {"locdate":20260706,"dateName":"제헌절","isHoliday":"Y"}
                }}}}
                """), HolidayType.NATIONAL);

        assertThat(holidays).hasSize(1);
        assertThat(holidays.get(0).getName()).isEqualTo("제헌절");
        // 쉬는 날 판정은 공휴일 오퍼레이션에만 적용된다
        assertThat(holidays.get(0).isRestDay()).isFalse();
    }

    @Test
    void parsesEmptyItemsWithoutFailing() {
        assertThat(HolidayApiClient.parse(json("""
                {"response":{"body":{"items":"","totalCount":0}}}
                """), HolidayType.SUNDRY)).isEmpty();
        assertThat(HolidayApiClient.parse(null, HolidayType.SUNDRY)).isEmpty();
    }

    private JsonNode json(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
