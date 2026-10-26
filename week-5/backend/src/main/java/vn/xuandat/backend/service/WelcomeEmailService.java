package vn.xuandat.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import vn.xuandat.backend.messaging.event.UserRegisteredEvent;

@Service
@RequiredArgsConstructor
public class WelcomeEmailService {
    private final JavaMailSender mailSender;

    public void sendWelcome(UserRegisteredEvent event) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom("no-reply@training.local");
        email.setTo(event.email());
        email.setSubject("Chào mừng bạn đến với Training Backend");
        email.setText("Xin chào " + event.fullName()
                + ",\n\nTài khoản của bạn đã được tạo thành công.");
        mailSender.send(email);
    }
}
