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
}
