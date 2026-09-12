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
import com.server.server.dto.filter.TransactionFilterRequest;
import com.server.server.dto.PageResponse;
import com.server.server.utilities.PaginationUtils;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.models.User;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.AccountRepository;
import com.server.server.repositories.TransactionRepository;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.exceptions.AccessDeniedException;

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

    @Transactional(readOnly = true)
    public Transaction getById(Integer id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        User currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !transaction.getAccount().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this transaction.");
        }
        return transaction;
    }

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
    public PageResponse<Transaction> filterTransactions(TransactionFilterRequest filter) {

        User currentUser = userService.getCurrentUser();

        var pageable = PaginationUtils.pageable(filter, "timestamp",
                Set.of("id", "timestamp", "amount", "type"), java.util.Map.of());

        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();

        if (filter.getType() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.getType()));
        if (filter.getStartDate() != null)
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), filter.getStartDate()));
        if (filter.getEndDate() != null)
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), filter.getEndDate()));
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("description")), term));
        }

        // 1. Customer Security: They can only see their own transactions
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), currentUser.getId()));
            return PageResponse.from(transactionRepository.findAll(spec, pageable));
        }

        // 2. Branch/Agency Filtering for Staff/Managers
        if (filter.getBranchId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agencyBranch").get("id"), filter.getBranchId()));
        else if (filter.getAgencyId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agency").get("id"), filter.getAgencyId()));
        else if (filter.getUserId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), filter.getUserId()));

        // 3. Fallback for Admin/General view
        return PageResponse.from(transactionRepository.findAll(spec, pageable));
    }
}
