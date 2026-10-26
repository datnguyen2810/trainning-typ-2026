package vn.xuandat.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.xuandat.backend.entity.RefreshToken;

import javax.swing.text.html.Option;
import java.sql.Ref;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
            SELECT t.user.id
            FROM RefreshToken t 
            WHERE t.tokenHash = :tokenHash
    """)
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true) //cho Spring Data biết đây là query cập nhật dữ liệu
    @Query("""
            UPDATE RefreshToken t
            SET t.revokedAt = :now
            WHERE t.familyId = :familyId
                AND t.revokedAt IS NULL
    """)
    int revokeFamily(@Param("familyId") UUID familyId,
                     @Param("now")Instant now);
}

