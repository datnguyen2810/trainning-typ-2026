package vn.xuandat.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {
    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;
}
