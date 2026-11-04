package com.server.server.repositories.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints; // IMPORTANT IMPORT

import com.server.server.enums.user.UserTypeEnum;
import com.server.server.models.user.User;
import com.server.server.utilities.MobileNumbers;
import jakarta.persistence.QueryHint; // IMPORTANT IMPORT

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserNameIgnoreCase(String userName);

    // --- THE LOOP KILLER ---
    // Forces Hibernate to run the SELECT query without flushing pending updates first
    @QueryHints(value = { @QueryHint(name = "org.hibernate.flushMode", value = "COMMIT") })
    Optional<User> findByEmailIgnoreCase(String email);



    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByMobileNumberAndIdNot(String mobileNumber, Integer id);

    List<User> findByIsActiveTrue();

    List<User> findByUserTypeAndIsActiveTrue(UserTypeEnum type);

    List<User> findByAgencyIdAndUserTypeAndIsActiveTrue(Integer agencyId, UserTypeEnum type);



    List<User> findByAgencyBranchIdAndUserTypeAndIsActiveTrue(Integer branchId, UserTypeEnum type);

    List<User> findByAgencyIdAndIsActiveTrue(Integer agencyId);
    
    
    Optional<User> findByEmailIgnoreCaseOrUserNameIgnoreCase(String email, String userName);

    Optional<User> findFirstByMobileNumberIn(Collection<String> mobileNumbers);

    /**
     * Sign-in accepts an email, a username, or a mobile number in any common format.
     * A phone number may be typed with spaces, dashes, or a leading 00 instead of +.
     */
    default Optional<User> findByLoginIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        Optional<User> byNameOrEmail = findByEmailIgnoreCaseOrUserNameIgnoreCase(value, value);
        if (byNameOrEmail.isPresent()) {
            return byNameOrEmail;
        }
        List<String> mobiles = MobileNumbers.loginCandidates(value);
        return mobiles.isEmpty() ? Optional.empty() : findFirstByMobileNumberIn(mobiles);
    }
}
