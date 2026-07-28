package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.email.EmailService;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.response.FindIdResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.auth.repository.VerificationCodeRedisRepository;

import java.security.SecureRandom;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindIdCommandService {

    private static final int CODE_LENGTH = 6;

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final VerificationCodeRedisRepository verificationCodeRedisRepository;
    private final EmailService emailService;

    /** 인증코드 발송 — 이름+이메일로 사용자 확인 후 6자리 코드를 이메일로 전송 */
    public void sendVerificationCode(String name, String email) {
        userRepository.findByNameAndEmail(name, email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND_BY_INFO));

        String code = generateCode();
        verificationCodeRedisRepository.save(email, code);
        emailService.sendVerificationCode(email, code);

        log.info("아이디 찾기 인증코드 발송 - email: {}", email);
    }

    /** 인증코드 검증 후 아이디 반환 */
    public FindIdResponse verifyCodeAndFindId(String email, String code) {
        String stored = verificationCodeRedisRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE));

        if (!stored.equals(code)) {
            throw new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE);
        }

        verificationCodeRedisRepository.deleteByEmail(email); // 재사용 방지

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        UserAuth userAuth = userAuthRepository.findByUserAndProvider(user, ProviderType.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        log.info("아이디 찾기 성공 - userId: {}", user.getUserId());
        return FindIdResponse.from(userAuth);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
