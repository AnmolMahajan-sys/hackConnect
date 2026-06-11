package com.hackconnect.repository;

import com.hackconnect.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    /** Groups the user is a member of */
    @Query("""
        SELECT g FROM Group g
        JOIN g.members m
        WHERE m.user.id = :userId
        ORDER BY g.createdAt DESC
        """)
    List<Group> findByMemberId(@Param("userId") Long userId);

    /** Public open groups — for discovery */
    List<Group> findByOpenTrueOrderByCreatedAtDesc();

    /** Groups for a specific domain */
    List<Group> findByDomainIgnoreCaseAndOpenTrueOrderByCreatedAtDesc(String domain);

    /** Groups linked to a hackathon name */
    List<Group> findByHackathonNameContainingIgnoreCaseAndOpenTrue(String hackathonName);
}
