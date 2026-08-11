package yooze.withme.domain.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.properties.KakaoProperties;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.client.dto.KakaoTokenResponse;
import yooze.withme.domain.auth.client.dto.KakaoUserInfoResponse;

/** 카카오 OAuth 서버(kauth, kapi)와 통신하는 클라이언트 */
@Slf4j
@Component
public class KakaoAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    public KakaoAuthClient(RestClient.Builder restClientBuilder, KakaoProperties kakaoProperties) {
        this.restClient = restClientBuilder.build();
        this.kakaoProperties = kakaoProperties;
    }

    /** 인가 코드(authorization code)로 카카오 access token 발급 */
    public KakaoTokenResponse getToken(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoProperties.clientId());
        body.add("redirect_uri", kakaoProperties.redirectUri());
        body.add("code", authorizationCode);
        if (StringUtils.hasText(kakaoProperties.clientSecret())) {
            body.add("client_secret", kakaoProperties.clientSecret());
        }

        try {
            return restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException e) {
            logKakaoError("토큰 발급", e);
            throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
        }
    }

    /** 카카오 access token으로 사용자 정보 조회 */
    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientException e) {
            logKakaoError("사용자 정보 조회", e);
            throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
        }
    }

    /** 카카오 API 실패 원인 진단용 로그 - 상태 코드와 응답 본문(에러 코드/메시지)까지 남긴다 */
    private void logKakaoError(String action, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            log.warn("[*] 카카오 {} 실패 : status={}, body={}",
                    action, responseException.getStatusCode(), responseException.getResponseBodyAsString());
        } else {
            log.warn("[*] 카카오 {} 실패 : {}", action, e.getMessage());
        }
    }
}
