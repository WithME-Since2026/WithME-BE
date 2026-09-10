package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                {"response":{"header":{"resultCode":"00","resultMsg":"OK"},"body":{"items":{"item":[
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
                {"response":{"header":{"resultCode":"00","resultMsg":"OK"},"body":{"items":{"item":
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
        // 결과 코드가 정상이면서 items 만 비어 있는 건 "그 해에 해당 특일이 없다"는 정상 응답이다
        assertThat(HolidayApiClient.parse(json("""
                {"response":{"header":{"resultCode":"00","resultMsg":"OK"},"body":{"items":"","totalCount":0}}}
                """), HolidayType.SUNDRY)).isEmpty();
    }

    @Test
    void failsOnErrorResultCode() {
        assertThatThrownBy(() -> HolidayApiClient.parse(json("""
                {"response":{"header":{"resultCode":"30","resultMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR"}}}
                """), HolidayType.HOLIDAY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("30")
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    void failsOnBodyWithoutHeader() {
        // 인증 오류는 다른 envelope 로 와서 header 자체가 없다
        assertThatThrownBy(() -> HolidayApiClient.parse(json("""
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"returnReasonCode":"22"}}}
                """), HolidayType.HOLIDAY))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> HolidayApiClient.parse(null, HolidayType.HOLIDAY))
                .isInstanceOf(IllegalStateException.class);
    }

    private JsonNode json(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
