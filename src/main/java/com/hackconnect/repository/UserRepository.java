package com.hackconnect.repository;

import com.hackconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Discover people — filter by domain interest, skill, college */
    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND u.id <> :currentUserId
          AND (:domain  IS NULL OR EXISTS (
                SELECT 1 FROM u.interests i WHERE LOWER(i) LIKE LOWER(CONCAT('%',:domain,'%'))))
          AND (:skill   IS NULL OR EXISTS (
                SELECT 1 FROM u.skills   s WHERE LOWER(s) LIKE LOWER(CONCAT('%',:skill,'%'))))
          AND (:college IS NULL OR LOWER(u.college) LIKE LOWER(CONCAT('%',:college,'%')))
        ORDER BY u.createdAt DESC
        """)
    List<User> discoverPeers(
            @Param("currentUserId") Long currentUserId,
            @Param("domain")  String domain,
            @Param("skill")   String skill,
            @Param("college") String college);
}
