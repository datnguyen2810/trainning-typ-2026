package vn.xuandat.backend.messaging.event;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId, // nhận diện sk
        UUID userId,  // nhận diện người dùng
        String email,
        String fullName,
        Instant occurredAt
) {
}
