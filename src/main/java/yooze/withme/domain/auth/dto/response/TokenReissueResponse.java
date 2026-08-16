package yooze.withme.domain.auth.dto.response;

public record TokenReissueResponse(
        String accessToken,
        String refreshToken
) {
}
