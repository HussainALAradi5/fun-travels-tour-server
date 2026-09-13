package com.server.server.utilities.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.server.dto.payment.PaymentResponse;
import com.server.server.dto.payment.TransactionResponse;
import com.server.server.dto.geography.CountryResponse;
import com.server.server.dto.geography.CityResponse;
import com.server.server.dto.geography.PortResponse;
import com.server.server.dto.notification.NotificationResponse;
import com.server.server.dto.notification.NotificationCounts;
import com.server.server.dto.support.UserRequestResponse;
import com.server.server.dto.support.CommentResponse;
import com.server.server.dto.support.EventLogResponse;
import com.server.server.dto.support.AccountResponse;
import com.server.server.models.Account;
import com.server.server.models.City;
import com.server.server.models.Country;
import com.server.server.models.GenericComment;
import com.server.server.models.GenericEventLog;
import com.server.server.models.Notification;
import com.server.server.models.Payment;
import com.server.server.models.Port;
import com.server.server.models.Transaction;
import com.server.server.models.UserRequest;

@Component
public class SystemMapper {

    public PaymentResponse toPaymentResponse(Payment p) {
        if (p == null) return null;
        PaymentResponse dto = new PaymentResponse();
        dto.setId(p.getId());
        dto.setAmount(p.getAmount());
        dto.setCurrency(p.getCurrency());
        dto.setMethod(p.getMethod());
        dto.setStatus(p.getStatus());
        dto.setTransactionId(p.getTransactionId());
        dto.setPaymentDate(p.getPaymentDate());
        if (p.getReservation() != null) {
            dto.setReservation(new PaymentResponse.ReservationSummary(
                    p.getReservation().getId(), p.getReservation().getReservationNumber()));
        }
        return dto;
    }

    public List<PaymentResponse> toPaymentResponseList(List<Payment> payments) {
        return payments.stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    public TransactionResponse toTransactionResponse(Transaction t) {
        if (t == null) return null;
        TransactionResponse dto = new TransactionResponse();
        dto.setId(t.getId());
        dto.setAmount(t.getAmount());
        dto.setTransactionType(t.getType());
        dto.setDescription(t.getDescription());
        dto.setTimestamp(t.getTimestamp());
        if (t.getAccount() != null) {
            dto.setAccount(new TransactionResponse.AccountSummary(
                    t.getAccount().getId(), t.getAccount().getAccountNumber(), t.getAccount().getAccountName()));
        }
        return dto;
    }

    public List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions) {
        return transactions.stream().map(this::toTransactionResponse).collect(Collectors.toList());
    }

    public CountryResponse toCountryResponse(Country c) {
        if (c == null) return null;
        return new CountryResponse(c.getId(), c.getFamousName(), c.getOfficialName(),
                c.getCountryCode(), c.getFlagPngUrl(), c.getFlagSvgUrl(), c.getDialCode(),
                c.getMobileNumberLength());
    }

    public List<CountryResponse> toCountryResponseList(List<Country> countries) {
        return countries.stream().map(this::toCountryResponse).collect(Collectors.toList());
    }

    public CityResponse toCityResponse(City c) {
        if (c == null) return null;
        CityResponse dto = new CityResponse();
        dto.setId(c.getId());
        dto.setName(c.getName());
        if (c.getCountry() != null) {
            dto.setCountry(new CityResponse.CountrySummary(c.getCountry().getId(), c.getCountry().getFamousName()));
        }
        return dto;
    }

    public List<CityResponse> toCityResponseList(List<City> cities) {
        return cities.stream().map(this::toCityResponse).collect(Collectors.toList());
    }

    public PortResponse toPortResponse(Port p) {
        if (p == null) return null;
        PortResponse dto = new PortResponse();
        dto.setId(p.getId());
        dto.setPortName(p.getPortName());
        dto.setPortCode(p.getPortCode());
        dto.setPortType(p.getPortType());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setStatus(p.getStatus());
        if (p.getCity() != null) {
            dto.setCity(new PortResponse.CitySummary(p.getCity().getId(), p.getCity().getName()));
        }
        if (p.getCountry() != null) {
            dto.setCountry(new PortResponse.CountrySummary(p.getCountry().getId(), p.getCountry().getFamousName()));
        }
        return dto;
    }

    public List<PortResponse> toPortResponseList(List<Port> ports) {
        return ports.stream().map(this::toPortResponse).collect(Collectors.toList());
    }

    public NotificationResponse toNotificationResponse(Notification n) {
        if (n == null) return null;
        NotificationResponse dto = new NotificationResponse();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setReferenceType(n.getReferenceType());
        dto.setReferenceId(n.getReferenceId());
        dto.setIsRead(n.getIsRead());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setReadAt(n.getReadAt());
        return dto;
    }

    public List<NotificationResponse> toNotificationResponseList(List<Notification> notifications) {
        return notifications.stream().map(this::toNotificationResponse).collect(Collectors.toList());
    }

    public NotificationCounts toNotificationCounts(long unreadCount) {
        return new NotificationCounts(unreadCount);
    }

    public UserRequestResponse toUserRequestResponse(UserRequest r) {
        if (r == null) return null;
        UserRequestResponse dto = new UserRequestResponse();
        dto.setId(r.getId());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setType(r.getType());
        dto.setStatus(r.getStatus());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        dto.setSolvedAt(r.getSolvedAt());
        if (r.getUser() != null) {
            dto.setUser(new UserRequestResponse.UserSummary(r.getUser().getId(), r.getUser().getName()));
        }
        if (r.getAssignedTo() != null) {
            dto.setAssignedTo(new UserRequestResponse.UserSummary(r.getAssignedTo().getId(), r.getAssignedTo().getName()));
        }
        if (r.getSolvedBy() != null) {
            dto.setSolvedBy(new UserRequestResponse.UserSummary(r.getSolvedBy().getId(), r.getSolvedBy().getName()));
        }
        return dto;
    }

    public List<UserRequestResponse> toUserRequestResponseList(List<UserRequest> requests) {
        return requests.stream().map(this::toUserRequestResponse).collect(Collectors.toList());
    }

    public CommentResponse toCommentResponse(GenericComment c) {
        if (c == null) return null;
        CommentResponse dto = new CommentResponse();
        dto.setId(c.getId());
        dto.setReferenceType(c.getReferenceType());
        dto.setReferenceId(c.getReferenceId());
        dto.setContent(c.getContent());
        dto.setCreatedAt(c.getCreatedAt());
        if (c.getAuthor() != null) {
            dto.setAuthor(new CommentResponse.UserSummary(c.getAuthor().getId(), c.getAuthor().getName()));
        }
        return dto;
    }

    public EventLogResponse toEventLogResponse(GenericEventLog e) {
        if (e == null) return null;
        EventLogResponse dto = new EventLogResponse();
        dto.setId(e.getId());
        dto.setReferenceType(e.getReferenceType());
        dto.setReferenceId(e.getReferenceId());
        dto.setAction(e.getAction());
        dto.setDescription(e.getDescription());
        dto.setCreatedAt(e.getCreatedAt());
        if (e.getActor() != null) {
            dto.setActor(new EventLogResponse.UserSummary(e.getActor().getId(), e.getActor().getName()));
        }
        return dto;
    }

    public AccountResponse toAccountResponse(Account a) {
        if (a == null) return null;
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getAccountName(),
                a.getBalance(), a.getType().name(), a.getStatus().name());
    }
}
