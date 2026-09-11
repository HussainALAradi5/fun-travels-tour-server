package com.server.server.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints; // IMPORTANT IMPORT
import org.springframework.stereotype.Repository;

import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;

import jakarta.persistence.QueryHint; // IMPORTANT IMPORT

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserNameIgnoreCase(String userName);

    // --- THE LOOP KILLER ---
    // Forces Hibernate to run the SELECT query without flushing pending updates first
    @QueryHints(value = { @QueryHint(name = "org.hibernate.flushMode", value = "COMMIT") })
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUserNameIgnoreCase(String userName);

    Optional<User> findByMobileNumber(String mobileNumber);

    List<User> findByIsActiveTrue();

    List<User> findByUserTypeAndIsActiveTrue(UserTypeEnum type);

    List<User> findByAgencyIdAndUserTypeAndIsActiveTrue(Integer agencyId, UserTypeEnum type);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    List<User> findByAgencyBranchIdAndUserTypeAndIsActiveTrue(Integer branchId, UserTypeEnum type);

    List<User> findByAgencyIdAndIsActiveTrue(Integer agencyId);
    
    Optional<User> findByEmailIgnoreCaseOrUserNameIgnoreCase(String email, String userName);
    
    Optional<User> findByEmailIgnoreCaseOrUserNameIgnoreCaseOrMobileNumber(String email, String userName, String mobile);
}
