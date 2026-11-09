package yooze.withme.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import yooze.withme.domain.auth.entity.User;

public record ProfileResponse(
        Long userId,
        String nickname,
        String profileImg,
        @JsonProperty("isKakaoLinked")
        boolean isKakaoLinked,
        /** 카카오 가입 시 저장한 이메일. 미연동이면 null */
        String kakaoEmail
) {

    public static ProfileResponse from(User user) {
        String kakaoEmail = user.isKakaoSync() ? user.getEmail() : null;
        return new ProfileResponse(
                user.getUserId(), user.getNickname(), user.getProfileImg(), user.isKakaoSync(), kakaoEmail);
    }
}
