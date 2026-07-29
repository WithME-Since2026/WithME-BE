package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import yooze.withme.domain.auth.repository.VerificationCodeRedisRepository;

import java.security.SecureRandom;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindAccountCommandService {

    private final UserAuthRepository userAuthRepository;
    private final VerificationCodeRedisRepository verificationCodeRedisRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserQueryService userQueryService;

    /** 아이디 찾기 - 닉네임+이메일로 사용자 확인 후 6자리 인증코드를 이메일로 발송 */
    public void sendFindIdCode(String nickname, String email) {
        userQueryService.getUserByNicknameAndEmail(nickname, email);

        String code = generateCode();
        verificationCodeRedisRepository.save(email, code);
        emailService.sendVerificationCode(email, code);

        log.info("아이디 찾기 인증코드 발송 - email: {}", email);
    }

    /** 아이디 찾기 - 인증코드 검증 후 아이디 반환 */
    public FindIdResponse verifyAndFindId(String email, String code) {
        String stored = verificationCodeRedisRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE));

        if (!stored.equals(code)) {
            throw new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE);
        }

        verificationCodeRedisRepository.deleteByEmail(email);

        User user = userQueryService.getUserByEmail(email);
        UserAuth userAuth = userAuthRepository.findByUserAndProvider(user, ProviderType.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        log.info("아이디 찾기 성공 - userId: {}", user.getUserId());
        return FindIdResponse.from(userAuth);
    }

    /** 비밀번호 찾기 - localId+이메일 일치 확인 후 6자리 인증코드를 이메일로 발송 */
    public void sendFindPasswordCode(String localId, String email) {
        UserAuth userAuth = userAuthRepository.findByLocalIdAndProvider(localId, ProviderType.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND_BY_INFO));

        if (!userAuth.getUser().getEmail().equals(email)) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND_BY_INFO);
        }

        String code = generateCode();
        verificationCodeRedisRepository.saveForPasswordReset(email, code);
        emailService.sendVerificationCode(email, code);

        log.info("비밀번호 찾기 인증코드 발송 - email: {}", email);
    }

    /** 비밀번호 찾기 - 인증코드 검증 후 비밀번호 재설정 */
    @Transactional
    public void resetPassword(String email, String code, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new GeneralException(ErrorStatus.PASSWORD_MISMATCH);
        }

        String stored = verificationCodeRedisRepository.findForPasswordReset(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE));

        if (!stored.equals(code)) {
            throw new GeneralException(ErrorStatus.INVALID_VERIFICATION_CODE);
        }

        verificationCodeRedisRepository.deleteForPasswordReset(email);

        UserAuth userAuth = userAuthRepository.findByUser_EmailAndProvider(email, ProviderType.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        userAuth.updatePassword(passwordEncoder.encode(newPassword));
        log.info("비밀번호 재설정 완료 - userId: {}", userAuth.getUser().getUserId());
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}
