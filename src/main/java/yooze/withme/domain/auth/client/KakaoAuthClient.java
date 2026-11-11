package yooze.withme.domain.auth.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

import java.time.Duration;

/** 카카오 OAuth 서버(kauth, kapi)와 통신하는 클라이언트 */
@Slf4j
@Component
public class KakaoAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URI = "https://kapi.kakao.com/v1/user/unlink";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    public KakaoAuthClient(KakaoProperties kakaoProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        this.kakaoProperties = kakaoProperties;
    }

    /**
     * 인가 코드(authorization code)로 카카오 access token 발급.
     * redirectUri 는 state 발급 시 플랫폼에 따라 선택된 값을 그대로 전달해야 한다.
     * 카카오는 인가 요청과 토큰 요청의 redirect_uri 가 다르면 오류를 반환한다.
     */
    public KakaoTokenResponse getToken(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoProperties.clientId());
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);
        if (StringUtils.hasText(kakaoProperties.clientSecret())) {
            body.add("client_secret", kakaoProperties.clientSecret());
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                log.warn("[*] 카카오 토큰 발급 응답이 비어있음");
                throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
            }
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("[*] 카카오 토큰 발급 실패 - 잘못된 인가 코드 : {}", e.getMessage());
                throw new GeneralException(ErrorStatus.KAKAO_TOKEN_REQUEST_FAILED);
            }
            log.warn("[*] 카카오 토큰 발급 실패 - 카카오 서버 오류 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.KAKAO_SERVER_ERROR);
        } catch (RestClientException e) {
            log.warn("[*] 카카오 토큰 발급 실패 - 네트워크 오류 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.KAKAO_SERVER_ERROR);
        }
    }

    /** 카카오 access token으로 사용자 정보 조회 */
    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null) {
                log.warn("[*] 카카오 사용자 정보 응답이 비어있음");
                throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
            }
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("[*] 카카오 사용자 정보 조회 실패 - 권한 없음 : {}", e.getMessage());
                throw new GeneralException(ErrorStatus.KAKAO_USER_INFO_REQUEST_FAILED);
            }
            log.warn("[*] 카카오 사용자 정보 조회 실패 - 카카오 서버 오류 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.KAKAO_SERVER_ERROR);
        } catch (RestClientException e) {
            log.warn("[*] 카카오 사용자 정보 조회 실패 - 네트워크 오류 : {}", e.getMessage());
            throw new GeneralException(ErrorStatus.KAKAO_SERVER_ERROR);
        }
    }

    /**
     * 회원 탈퇴 시 앱 연결 끊기 (Admin 키 방식).
     * 탈퇴 커밋 이후에 호출되므로 실패해도 예외를 던지지 않고 로그만 남긴다.
     */
    public void unlink(Long kakaoId) {
        if (!StringUtils.hasText(kakaoProperties.adminKey())) {
            log.warn("[*] 카카오 Admin 키 미설정 - unlink 생략, kakaoId: {}", kakaoId);
            return;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", String.valueOf(kakaoId));

        try {
            restClient.post()
                    .uri(UNLINK_URI)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("[*] 카카오 unlink 실패 - kakaoId: {} : {}", kakaoId, e.getMessage());
        }
    }
}
