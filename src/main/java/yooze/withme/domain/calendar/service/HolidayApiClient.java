package yooze.withme.domain.calendar.service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import yooze.withme.common.properties.HolidayProperties;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.enums.HolidayType;

/** 공공데이터포털 「특일 정보」 조회. 5개 오퍼레이션을 연 단위로 부른다. */
@Component
public class HolidayApiClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/";
    private static final DateTimeFormatter LOCDATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    // ponytail: 오퍼레이션당 연 100건을 넘지 않아 한 페이지로 끝난다. 넘치면 pageNo 루프를 돌 것.
    private static final int NUM_OF_ROWS = 200;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final HolidayProperties properties;
    private final RestClient restClient;

    public HolidayApiClient(HolidayProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** 해당 연도의 특일을 오퍼레이션 하나 분량 가져온다. */
    public List<Holiday> fetch(HolidayType type, int year) {
        JsonNode response = restClient.get()
                // 발급 키는 이미 URL 인코딩된 문자열이라 UriBuilder에 태우면 이중 인코딩된다
                .uri(URI.create(BASE_URL + operationOf(type)
                        + "?serviceKey=" + properties.serviceKey()
                        + "&solYear=" + year
                        + "&numOfRows=" + NUM_OF_ROWS
                        + "&_type=json"))
                .retrieve()
                .body(JsonNode.class);

        return parse(response, type);
    }

    /** 정상 코드는 "00". 인증키 오류 등은 2xx 로 오면서 본문에만 코드가 실린다. */
    private static final String RESULT_CODE_OK = "00";

    /** 응답 형태(빈 결과, 단건 객체, 배열)를 흡수하는 지점이라 패키지 공개로 두고 직접 테스트한다. */
    static List<Holiday> parse(JsonNode response, HolidayType type) {
        // 오류 본문도 200 으로 오므로 결과 코드를 확인하지 않으면 빈 목록과 구분되지 않는다
        if (response == null) {
            throw new IllegalStateException("공휴일 API 응답 본문이 비어 있습니다");
        }
        JsonNode header = response.path("response").path("header");
        String resultCode = header.path("resultCode").asText("");
        if (!RESULT_CODE_OK.equals(resultCode)) {
            throw new IllegalStateException("공휴일 API 오류 응답 : resultCode=%s, msg=%s"
                    .formatted(resultCode, header.path("resultMsg").asText("")));
        }

        List<Holiday> holidays = new ArrayList<>();
        // 결과가 없으면 items 가 객체가 아니라 빈 문자열로 온다
        JsonNode items = response.path("response").path("body").path("items").path("item");
        if (items.isMissingNode() || items.isNull()) {
            return holidays;
        }

        // 1건이면 배열이 아니라 객체 하나로 온다
        for (JsonNode item : items.isArray() ? items : List.of(items)) {
            String locdate = item.path("locdate").asText(null);
            String name = item.path("dateName").asText(null);
            if (locdate == null || name == null || name.isBlank()) {
                continue;
            }
            holidays.add(Holiday.builder()
                    .date(LocalDate.parse(locdate, LOCDATE))
                    .name(name.strip())
                    .type(type)
                    // 쉬는 날 여부는 공휴일 오퍼레이션만 의미가 있다(제헌절 같은 국경일은 쉬지 않는다)
                    .restDay(type == HolidayType.HOLIDAY
                            && "Y".equalsIgnoreCase(item.path("isHoliday").asText()))
                    .build());
        }
        return holidays;
    }

    private static String operationOf(HolidayType type) {
        return switch (type) {
            case HOLIDAY -> "getRestDeInfo";
            case NATIONAL -> "getHoliDeInfo";
            case ANNIVERSARY -> "getAnniversaryInfo";
            case SOLAR_TERM -> "get24DivisionsInfo";
            case SUNDRY -> "getSundryDayInfo";
        };
    }
}
