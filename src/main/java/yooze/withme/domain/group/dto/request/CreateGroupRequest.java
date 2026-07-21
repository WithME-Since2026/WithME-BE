package yooze.withme.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateGroupRequest(
        @NotBlank(message = "모임 이름은 필수입니다.")
        String name,

        @NotBlank(message = "모임 소개는 필수입니다.")
        String intro,

        boolean remindOffer,

        @NotNull(message = "시작 날짜는 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        LocalDate endDate,

        @NotBlank(message = "장소 이름은 필수입니다.")
        String locationName,

        @NotBlank(message = "장소 주소는 필수입니다.")
        String locationAddress,

        Long placeId
) {
}
