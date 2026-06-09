package com.example.bsep.repository;

import com.example.bsep.model.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {

    Optional<SessionToken> findByJti(String jti);

    List<SessionToken> findByUserEmailAndRevokedFalse(String userEmail);
}