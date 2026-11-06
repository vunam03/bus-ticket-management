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
        send(email, "Đặt lại mật khẩu", "Link hiệu lực 15 phút và dùng một lần.\n\n"
                + resetUrl + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua thư này.");
    }

    @Override
    public void sendEmailVerification(String email, String verificationUrl, boolean emailChange) {
        String title = emailChange ? "Xác nhận địa chỉ email mới" : "Xác thực email đăng ký";
        send(email, title, "Link hiệu lực " + (emailChange ? "30 phút" : "24 giờ")
                + ", chỉ dùng một lần.\nMở link rồi bấm Xác nhận email để hoàn tất.\n\n"
                + verificationUrl + "\n\nNếu không phải bạn yêu cầu, hãy bỏ qua thư này.");
    }

    private void send(String email, String subject, String body) {
        JavaMailSender configured = sender.getIfAvailable();
        if (configured == null) throw new IllegalStateException("SMTP chưa được cấu hình.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("BusBooking - " + subject);
        message.setText(body);
        configured.send(message);
    }
}
