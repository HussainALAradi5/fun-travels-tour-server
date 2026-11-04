package com.server.server.repositories.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.server.server.models.payment.Transaction;
public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {
    
    // Add this line to fix the "undefined" error

    List<Transaction> findByAccountIdOrderByTimestampDesc(Integer accountId);
    

    
}
