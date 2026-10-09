
package com.smartmove.backend.repository;

import com.smartmove.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserAccount> findByPassengerId(Long passengerId);

    Optional<UserAccount> findByDriverId(Long driverId);

    List<UserAccount> findByRole(String role);

    List<UserAccount> findByAccountStatus(String accountStatus);

    List<UserAccount> findByRoleAndAccountStatus(
            String role,
            String accountStatus
    );
}
