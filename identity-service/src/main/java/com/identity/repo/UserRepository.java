package com.identity.repo;


import com.identity.entity.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    @Modifying
    @Query("DELETE FROM User u WHERE u.verified = false AND u.createdAt < :time")
    int  deleteByVerifiedFalseAndCreatedAtBefore(@Param("time") Instant time);
}
