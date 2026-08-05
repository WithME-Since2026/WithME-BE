package yooze.withme.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /** 아이디 찾기 인증코드 이메일 발송 */
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("[WithME] 아이디 찾기 인증코드");
            helper.setText(buildCodeEmailBody(code), true);

            mailSender.send(message);
            log.info("인증코드 이메일 발송 완료 - to: {}", maskEmail(to));
        } catch (MessagingException e) {
            log.error("인증코드 이메일 발송 실패 - to: {}", maskEmail(to), e);
            throw new GeneralException(ErrorStatus.EMAIL_SEND_FAILED);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private String buildCodeEmailBody(String code) {
        return """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #4A90E2;">WithME 아이디 찾기</h2>
                  <p>아래 인증코드를 5분 이내에 입력해 주세요.</p>
                  <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                              background: #f4f4f4; padding: 20px; text-align: center;
                              border-radius: 8px; margin: 20px 0;">
                    %s
                  </div>
                  <p style="color: #888; font-size: 13px;">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(code);
    }
}
