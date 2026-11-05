package com.server.server.services.payment;

import com.server.server.exceptions.WorkflowException;

import com.server.server.services.auth.AccessPolicyService;

import com.server.server.services.notification.NotificationService;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.enums.payment.TransactionType;
import com.server.server.dto.payment.TransactionFilterRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.utilities.PaginationUtils;
import com.server.server.utilities.FilterUtils;
import com.server.server.models.payment.Account;
import com.server.server.models.payment.Transaction;
import com.server.server.models.user.User;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.payment.AccountRepository;
import com.server.server.repositories.payment.TransactionRepository;
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
    private AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public Transaction getById(Integer id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        User currentUser = accessPolicy.requireCurrentUser();
        TourReservation reservation = transaction.getReservation();
        if (AccessPolicyService.isCustomer(currentUser) || reservation == null) {
            if (!AccessPolicyService.isAdmin(currentUser)
                    && !transaction.getAccount().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to view this transaction.");
            }
        } else {
            accessPolicy.requireAgencyAccess(currentUser, reservation.getTour().getAgency());
        }
        return transaction;
    }

    @Transactional
    public Transaction creditAccount(Account account, BigDecimal amount, TransactionType type, String description,
            TourReservation reservation) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero.");
        }
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
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero.");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new WorkflowException("INSUFFICIENT_BALANCE", "Insufficient wallet balance.");
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

        User currentUser = accessPolicy.requireCurrentUser();
        var pageable = PaginationUtils.pageable(filter, "timestamp",
                Set.of("id", "timestamp", "amount", "type"), java.util.Map.of());

        Specification<Transaction> spec = (root, query, cb) -> cb.conjunction();

        if (filter.getType() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.getType()));
        spec = spec.and(FilterUtils.localDateTimeRange("timestamp", filter.getStartDate(), filter.getEndDate()));
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("description")), term));
        }

        // Customers only see their own entries.
        if (AccessPolicyService.isCustomer(currentUser)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), currentUser.getId()));
            return PageResponse.from(transactionRepository.findAll(spec, pageable));
        }

        Integer agencyScope = AccessPolicyService.agencyScope(currentUser);
        if (agencyScope != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agency").get("id"), agencyScope));
        }
        if (filter.getBranchId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agencyBranch").get("id"), filter.getBranchId()));
        else if (filter.getAgencyId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservation").get("tour").get("agency").get("id"), filter.getAgencyId()));
        else if (filter.getUserId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), filter.getUserId()));

        return PageResponse.from(transactionRepository.findAll(spec, pageable));
    }
}
