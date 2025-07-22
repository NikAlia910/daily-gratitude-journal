package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.GratitudeEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the GratitudeEntry entity.
 */
@Repository
public interface GratitudeEntryRepository extends JpaRepository<GratitudeEntry, Long> {
    @Query("select gratitudeEntry from GratitudeEntry gratitudeEntry where gratitudeEntry.user.login = ?#{authentication.name}")
    List<GratitudeEntry> findByUserIsCurrentUser();

    default Optional<GratitudeEntry> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<GratitudeEntry> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<GratitudeEntry> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select gratitudeEntry from GratitudeEntry gratitudeEntry left join fetch gratitudeEntry.user",
        countQuery = "select count(gratitudeEntry) from GratitudeEntry gratitudeEntry"
    )
    Page<GratitudeEntry> findAllWithToOneRelationships(Pageable pageable);

    @Query("select gratitudeEntry from GratitudeEntry gratitudeEntry left join fetch gratitudeEntry.user")
    List<GratitudeEntry> findAllWithToOneRelationships();

    @Query("select gratitudeEntry from GratitudeEntry gratitudeEntry left join fetch gratitudeEntry.user where gratitudeEntry.id =:id")
    Optional<GratitudeEntry> findOneWithToOneRelationships(@Param("id") Long id);
}
