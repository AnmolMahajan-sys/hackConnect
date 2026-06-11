package com.hackconnect.repository;

import com.hackconnect.model.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    @Query("""
        SELECT o FROM Opportunity o
        WHERE (:type     IS NULL OR o.type   = :type)
          AND (:domain   IS NULL OR LOWER(o.domain) = LOWER(:domain))
          AND (:online   IS NULL OR o.online = :online)
          AND (:verified IS NULL OR o.verified = :verified)
          AND (:search   IS NULL
               OR LOWER(o.title)     LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.organizer) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.domain)    LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:deadlineAfter IS NULL OR o.deadline >= :deadlineAfter)
        """)
    Page<Opportunity> filter(
            @Param("type")          Opportunity.Type type,
            @Param("domain")        String domain,
            @Param("online")        Boolean online,
            @Param("verified")      Boolean verified,
            @Param("search")        String search,
            @Param("deadlineAfter") LocalDateTime deadlineAfter,
            Pageable pageable);

    List<Opportunity> findTop6ByVerifiedTrueOrderByViewCountDesc();

    @Query("SELECT o FROM Opportunity o WHERE o.deadline > :now ORDER BY o.deadline ASC")
    List<Opportunity> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("UPDATE Opportunity o SET o.viewCount = o.viewCount + 1 WHERE o.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT DISTINCT o.domain FROM Opportunity o ORDER BY o.domain")
    List<String> findAllDomains();

    @Modifying
    @Query(value = "DELETE FROM hc_opp_tags", nativeQuery = true)
    void deleteAllTags();

    @Modifying
    @Query(value = "DELETE FROM hc_opps", nativeQuery = true)
    void deleteAllOpportunities();
}