package com.busbooking.bus_ticket_management.service;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class SmtpResetMailService implements ResetMailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public SmtpResetMailService(ObjectProvider<JavaMailSender> sender,
                               @Value("${app.mail.from:noreply@localhost}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        JavaMailSender configured = sender.getIfAvailable();
        if (configured == null) {
            throw new IllegalStateException("SMTP chưa được cấu hình.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("BusBooking - Đặt lại mật khẩu");
        message.setText("Link có hiệu lực 15 phút và chỉ dùng một lần.\n\n"
                + resetUrl + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua email này.");
        configured.send(message);
    }
}
