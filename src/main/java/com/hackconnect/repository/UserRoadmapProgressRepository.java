package com.hackconnect.repository;

import com.hackconnect.model.LearningRoadmap;
import com.hackconnect.model.UserRoadmapProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoadmapProgressRepository extends JpaRepository<UserRoadmapProgress, Long> {

    List<UserRoadmapProgress> findByUserIdAndRoadmapId(Long userId, Long roadmapId);

    List<UserRoadmapProgress> findByUserId(Long userId);

    Optional<UserRoadmapProgress> findByUserIdAndStepId(Long userId, Long stepId);

    boolean existsByUserIdAndRoadmapId(Long userId, Long roadmapId);

    /**
     * Count completed steps for a user in a roadmap.
     * FIX: Hibernate 6 cannot resolve fully-qualified enum paths in JPQL.
     *      Use a named string literal that matches the @Enumerated(STRING) value.
     */
    @Query("""
        SELECT COUNT(p) FROM UserRoadmapProgress p
        WHERE p.user.id    = :userId
          AND p.roadmap.id = :roadmapId
          AND p.status     = 'COMPLETED'
        """)
    long countCompleted(@Param("userId") Long userId, @Param("roadmapId") Long roadmapId);

    /** All roadmaps the user has enrolled in (distinct) */
    @Query("""
        SELECT DISTINCT p.roadmap FROM UserRoadmapProgress p
        WHERE p.user.id = :userId
        """)
    List<LearningRoadmap> findEnrolledRoadmaps(@Param("userId") Long userId);
}
