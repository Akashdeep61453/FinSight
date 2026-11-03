package com.financeinsight.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query(value = "WITH registration_lock AS (SELECT pg_advisory_xact_lock(713901)) SELECT TRUE FROM registration_lock",
            nativeQuery = true)
    Boolean lockRegistration();

    @Query(value = "UPDATE statements SET owner_id = :ownerId WHERE owner_id IS NULL", nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    int claimLegacyStatements(@Param("ownerId") UUID ownerId);
}
