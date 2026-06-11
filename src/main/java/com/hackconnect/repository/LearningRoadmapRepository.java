package com.hackconnect.repository;

import com.hackconnect.model.LearningRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, Long> {

    List<LearningRoadmap> findByDomainIgnoreCase(String domain);

    List<LearningRoadmap> findByLevel(LearningRoadmap.Level level);

    List<LearningRoadmap> findByDomainIgnoreCaseAndLevel(String domain, LearningRoadmap.Level level);

    List<LearningRoadmap> findTop6ByOrderByEnrolledCountDesc();

    @Query("SELECT DISTINCT r.domain FROM LearningRoadmap r ORDER BY r.domain")
    List<String> findAllDomains();

    @Modifying
    @Query("UPDATE LearningRoadmap r SET r.enrolledCount = r.enrolledCount + 1 WHERE r.id = :id")
    void incrementEnrolledCount(@Param("id") Long id);
}
