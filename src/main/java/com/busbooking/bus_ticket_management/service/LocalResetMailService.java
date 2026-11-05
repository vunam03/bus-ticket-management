package com.busbooking.bus_ticket_management.service;


import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/** Development mailbox: local files, never a public HTTP endpoint. */
@Service
@Profile("local")
public class LocalResetMailService implements ResetMailService {
    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        try {
            Path directory = Path.of("work", "mail-outbox");
            Files.createDirectories(directory);
            String body = "To: " + email + "\nSubject: BusBooking - Đặt lại mật khẩu\n"
                    + "Date: " + Instant.now() + "\n\nLink hiệu lực 15 phút, dùng một lần:\n" + resetUrl;
            Files.writeString(directory.resolve(UUID.randomUUID() + ".txt"), body, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể ghi hộp thư demo.", exception);
        }
    }
}
