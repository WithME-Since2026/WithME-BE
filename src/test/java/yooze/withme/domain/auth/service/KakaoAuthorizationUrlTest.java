package yooze.withme.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.common.properties.KakaoProperties;
import yooze.withme.domain.auth.dto.response.KakaoStateResponse;
import yooze.withme.domain.auth.enums.PlatformType;
import yooze.withme.domain.auth.repository.OAuthStateRedisRepository;

@ExtendWith(MockitoExtension.class)
class KakaoAuthorizationUrlTest {

    @Mock private KakaoProperties kakaoProperties;
    @Mock private OAuthStateRedisRepository stateRepository;
    @InjectMocks private AuthCommandService service;

    @ParameterizedTest
    @EnumSource(PlatformType.class)
    @NullSource
    void redirectUriSurvivesQueryDecodingWithoutChangingOtherParameters(PlatformType platform) {
        String redirectUri = "https://example.com/콜백?next=%2Fhome&value=a+b=c#fragment";
        when(kakaoProperties.redirectUriFor(platform)).thenReturn(redirectUri);
        when(kakaoProperties.clientId()).thenReturn("client-id");

        KakaoStateResponse response = service.generateKakaoState(platform);

        URI uri = URI.create(response.kakaoLoginUrl());
        Map<String, String> query = Arrays.stream(uri.getRawQuery().split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> part[0],
                        part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("kauth.kakao.com");
        assertThat(uri.getPath()).isEqualTo("/oauth/authorize");
        assertThat(uri.getFragment()).isNull();
        assertThat(query).containsExactlyInAnyOrderEntriesOf(Map.of(
                "client_id", "client-id", "redirect_uri", redirectUri,
                "response_type", "code", "state", response.state()));
        verify(stateRepository).save(response.state(), redirectUri);
    }
}
