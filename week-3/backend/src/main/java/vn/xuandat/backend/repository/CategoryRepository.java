package vn.xuandat.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.xuandat.backend.entity.Category;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String normalizedSlug);

    boolean existsBySlugAndIdNot(String normalizedSlug, UUID id);

    @Modifying
    @Query("""
        update Category c
        set c.active = false,
            c.updatedAt = :updatedAt
        where c.id = :id
            and c.active = true
    """)
    int deactiveById(@Param("id") UUID id,
                     @Param("updatedAt") Instant updatedAt);

    Optional<Category> findByIdAndActiveTrue(UUID id);

    @Query("""
        select c
        from Category c
        where c.active = true
        order by c.slug asc
        limit :size offset :offset
    """)
    List<Category> getCategories(@Param("offset") long offset,
                                 @Param("size") int size);

    long countByActiveTrue();
}
