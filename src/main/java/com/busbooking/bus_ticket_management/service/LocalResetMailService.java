package com.busbooking.bus_ticket_management.service;


import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/** Development mailbox stays on disk and is never served as a public endpoint. */
@Service
@Profile("local")
public class LocalResetMailService implements ResetMailService {
    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        write(email, "Đặt lại mật khẩu", "Link hiệu lực 15 phút, dùng một lần:\n" + resetUrl);
    }

    @Override
    public void sendEmailVerification(String email, String verificationUrl, boolean emailChange) {
        String title = emailChange ? "Xác nhận địa chỉ email mới" : "Xác thực email đăng ký";
        String duration = emailChange ? "30 phút" : "24 giờ";
        write(email, title, "Link hiệu lực " + duration + ", chỉ dùng một lần.\n"
                + "Mở link rồi bấm Xác nhận email để hoàn tất.\n" + verificationUrl
                + "\nNếu không phải bạn yêu cầu, hãy bỏ qua thư này.");
    }

    private void write(String email, String subject, String content) {
        try {
            Path directory = Path.of("work", "mail-outbox");
            Files.createDirectories(directory);
            String body = "To: " + email + "\nSubject: BusBooking - " + subject
                    + "\nDate: " + Instant.now() + "\n\n" + content;
            Files.writeString(directory.resolve(UUID.randomUUID() + ".txt"), body, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể ghi hộp thư demo.", exception);
        }
    }
}
