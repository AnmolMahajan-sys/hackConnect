package com.hackconnect.repository;

import com.hackconnect.model.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    /** Latest messages for a group, newest first — reverse in service */
    List<GroupMessage> findByGroupIdOrderBySentAtDesc(Long groupId, Pageable pageable);

    /** Count unread (simplified: total messages after a cursor) */
    long countByGroupId(Long groupId);
}
