package com.server.server.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.TransactionType;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.models.User;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.AccountRepository;
import com.server.server.repositories.TransactionRepository;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserService userService;

    @Transactional
    public Transaction creditAccount(Account account, BigDecimal amount, TransactionType type, String description,
            TourReservation reservation) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .account(account).amount(amount).type(type).description(description).reservation(reservation).build();

        tx = transactionRepository.save(tx);

        notificationService.sendNotification(
                account.getUser(), "Wallet Credited",
                String.format("Your wallet was credited by $%.2f. Reason: %s", amount, description),
                NotificationType.SYSTEM_ALERT, tx.getId(), ReferenceType.USER);

        return tx;
    }

    @Transactional
    public Transaction debitAccount(Account account, BigDecimal amount, TransactionType type, String description,
            TourReservation reservation) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance.");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .account(account).amount(amount.negate()).type(type).description(description).reservation(reservation)
                .build();

        tx = transactionRepository.save(tx);

        notificationService.sendNotification(
                account.getUser(), "Wallet Debited",
                String.format("Your wallet was debited by $%.2f. Reason: %s", amount, description),
                NotificationType.SYSTEM_ALERT, tx.getId(), ReferenceType.USER);

        return tx;
    }

    @Transactional(readOnly = true)
    public List<Transaction> filterTransactions(
            Integer userId, TransactionType type,
            LocalDateTime startDate, LocalDateTime endDate,
            Long agencyId, Long branchId,
            String sortBy, String sortDir) {

        User currentUser = userService.getCurrentUser();

        // Default sorting by timestamp if none provided
        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                (sortBy == null || sortBy.isBlank()) ? "timestamp" : sortBy);

        // 1. Customer Security: They can only see their own transactions
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) {
            return transactionRepository.findByAccount_User_Id(currentUser.getId(), sort);
        }

        // 2. Branch/Agency Filtering for Staff/Managers
        if (branchId != null) {
            return transactionRepository.findByReservation_Tour_AgencyBranch_Id(branchId, sort);
        }

        if (agencyId != null) {
            return transactionRepository.findByReservation_Tour_Agency_Id(agencyId, sort);
        }

        // 3. Fallback for Admin/General view
        return transactionRepository.findAll(sort);
    }
}