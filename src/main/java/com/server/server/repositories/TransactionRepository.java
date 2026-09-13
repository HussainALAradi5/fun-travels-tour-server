package com.server.server.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.server.server.enums.TransactionType;
import com.server.server.models.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {
    
    // Add this line to fix the "undefined" error
    List<Transaction> findByAccount_User_Id(Integer userId, Sort sort);

    List<Transaction> findByAccountIdOrderByTimestampDesc(Integer accountId);
    
    List<Transaction> findByAccount_User_IdAndTypeAndTimestampBetween(
            Integer userId, TransactionType type, LocalDateTime start, LocalDateTime end, Sort sort);

    List<Transaction> findByReservation_Tour_Agency_Id(Long agencyId, Sort sort);
    
    List<Transaction> findByReservation_Tour_AgencyBranch_Id(Long branchId, Sort sort);
}
