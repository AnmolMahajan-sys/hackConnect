package com.hackconnect.repository;

import com.hackconnect.model.ConnectionRequest;
import com.hackconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    List<ConnectionRequest> findByReceiverAndStatus(User receiver, ConnectionRequest.Status status);

    List<ConnectionRequest> findBySenderAndStatus(User sender, ConnectionRequest.Status status);

    Optional<ConnectionRequest> findBySenderAndReceiver(User sender, User receiver);

    boolean existsBySenderAndReceiver(User sender, User receiver);

    /** All accepted connections for a user (as either sender or receiver) */
    @Query("""
        SELECT c FROM ConnectionRequest c
        WHERE c.status = 'ACCEPTED'
          AND (c.sender.id = :userId OR c.receiver.id = :userId)
        """)
    List<ConnectionRequest> findAcceptedConnections(@Param("userId") Long userId);

    /** Check if two users are already connected */
    @Query("""
        SELECT COUNT(c) > 0 FROM ConnectionRequest c
        WHERE c.status = 'ACCEPTED'
          AND ((c.sender.id = :a AND c.receiver.id = :b)
            OR (c.sender.id = :b AND c.receiver.id = :a))
        """)
    boolean areConnected(@Param("a") Long a, @Param("b") Long b);
}
