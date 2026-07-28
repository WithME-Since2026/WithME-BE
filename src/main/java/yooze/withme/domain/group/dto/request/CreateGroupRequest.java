package yooze.withme.domain.group.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateGroupRequest(
        @NotBlank(message = "모임 이름은 필수입니다.")
        @Size(max = 127, message = "모임 이름은 127자를 넘을 수 없습니다.")
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
        @Size(max = 127, message = "장소 이름은 127자를 넘을 수 없습니다.")
        String locationName,

        @NotBlank(message = "장소 주소는 필수입니다.")
        @Size(max = 255, message = "장소 주소는 255자를 넘을 수 없습니다.")
        String locationAddress,

        @NotNull(message = "장소 ID는 필수입니다.")
        Long placeId
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "종료 날짜는 시작 날짜보다 이후여야 합니다.")
    public boolean getEndDateValid() {
        if (endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
