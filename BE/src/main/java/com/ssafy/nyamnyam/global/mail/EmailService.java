package com.ssafy.nyamnyam.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String senderEmail;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${spring.mail.username:}") String senderEmail
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.senderEmail = senderEmail;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = frontendUrl + "/auth/email/verify?token=" + token;

        log.info("=========================================================");
        log.info("[EmailService] 회원가입 인증 메일 발송 대상: {}", toEmail);
        log.info("[EmailService] 인증 토큰: {}", token);
        log.info("[EmailService] 인증 링크: {}", verificationUrl);
        log.info("=========================================================");

        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            log.warn("[EmailService] 메일 설정(SMTP)이 활성화되지 않아 콘솔 로그로 대체합니다.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[냠냠취미] 이메일 인증을 완료해 주세요.");
            message.setText("안녕하세요! 냠냠취미에 오신 것을 환영합니다.\n\n"
                    + "아래 링크를 클릭하여 이메일 인증을 완료해 주세요:\n"
                    + verificationUrl + "\n\n"
                    + "본 링크는 24시간 동안 유효합니다.");

            mailSender.send(message);
            log.info("[EmailService] 메일 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 중 오류가 발생했습니다: {}", e.getMessage());
            // 로컬 테스트 연속성을 위해 예외를 던지지 않고 로그로 남김
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontendUrl + "/auth/password/reset?token=" + token;

        log.info("=========================================================");
        log.info("[EmailService] 비밀번호 재설정 메일 발송 대상: {}", toEmail);
        log.info("[EmailService] 재설정 토큰: {}", token);
        log.info("[EmailService] 재설정 링크: {}", resetUrl);
        log.info("=========================================================");

        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            log.warn("[EmailService] 메일 설정(SMTP)이 활성화되지 않아 콘솔 로그로 대체합니다.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[냠냠취미] 비밀번호 재설정 링크를 안내해 드립니다.");
            message.setText("안녕하세요, 냠냠취미입니다.\n\n"
                    + "아래 링크를 클릭하여 새로운 비밀번호를 설정해 주세요:\n"
                    + resetUrl + "\n\n"
                    + "본 링크는 30분 동안 유효합니다.");

            mailSender.send(message);
            log.info("[EmailService] 비밀번호 재설정 메일 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 중 오류가 발생했습니다: {}", e.getMessage());
        }
    }

    public void sendGoogleAccountNoticeEmail(String toEmail) {
        log.info("=========================================================");
        log.info("[EmailService] Google 연동 안내 메일 발송 대상: {}", toEmail);
        log.info("=========================================================");

        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            log.warn("[EmailService] 메일 설정(SMTP)이 활성화되지 않아 콘솔 로그로 대체합니다.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("[냠냠취미] 소셜 계정 로그인 안내");
            message.setText("안녕하세요, 냠냠취미입니다.\n\n"
                    + "요청하신 계정은 Google 소셜 로그인으로 가입된 계정입니다.\n"
                    + "비밀번호 재설정 대신 Google 로그인을 이용해 주세요.\n\n"
                    + "감사합니다.");

            mailSender.send(message);
            log.info("[EmailService] Google 연동 안내 메일 발송 성공: {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 중 오류가 발생했습니다: {}", e.getMessage());
        }
    }
}
