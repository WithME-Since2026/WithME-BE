package yooze.withme.domain.group.dto.request;

import jakarta.validation.constraints.NotNull;

public record DelegateOwnerRequest(
        @NotNull(message = "새 모임장 ID는 필수입니다.")
        Long newOwnerId
) {
}
