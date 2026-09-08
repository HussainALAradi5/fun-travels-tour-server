package com.server.server.utilities;

import java.util.List;

import org.springframework.stereotype.Component;

import com.server.server.dto.agency.AgencyBranchResponse;
import com.server.server.dto.agency.AgencyResponse;
import com.server.server.dto.geography.CityResponse;
import com.server.server.dto.geography.CountryResponse;
import com.server.server.dto.geography.PortResponse;
import com.server.server.dto.notification.NotificationCounts;
import com.server.server.dto.notification.NotificationResponse;
import com.server.server.dto.payment.PaymentResponse;
import com.server.server.dto.payment.TransactionResponse;
import com.server.server.dto.support.AccountResponse;
import com.server.server.dto.support.CommentResponse;
import com.server.server.dto.support.EventLogResponse;
import com.server.server.dto.support.UserRequestResponse;
import com.server.server.dto.tour.MealPlanResponse;
import com.server.server.dto.tour.ReservationResponse;
import com.server.server.dto.tour.SeatResponse;
import com.server.server.dto.tour.TicketResponse;
import com.server.server.dto.tour.TourResponse;
import com.server.server.dto.tour.TransportationResponse;
import com.server.server.dto.user.UserResponse;
import com.server.server.models.Agency.Agency;
import com.server.server.models.Agency.AgencyBranch;
import com.server.server.utilities.mappers.SystemMapper;
import com.server.server.utilities.mappers.TourMapper;
import com.server.server.utilities.mappers.UserMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModelMapper {

    private final UserMapper userMapper;
    private final TourMapper tourMapper;
    private final SystemMapper systemMapper;

    // ========== USER ==========
    public UserResponse toUserResponse(User user) { return userMapper.toUserResponse(user); }
    public List<UserResponse> toUserResponseList(List<User> users) { return userMapper.toUserResponseList(users); }

    // ========== AGENCY ==========
    public AgencyResponse toAgencyResponse(Agency agency) { return userMapper.toAgencyResponse(agency); }
    public List<AgencyResponse> toAgencyResponseList(List<Agency> agencies) { return userMapper.toAgencyResponseList(agencies); }
    public AgencyBranchResponse toBranchResponse(AgencyBranch branch) { return userMapper.toBranchResponse(branch); }
    public List<AgencyBranchResponse> toBranchResponseList(List<AgencyBranch> branches) { return userMapper.toBranchResponseList(branches); }

    // ========== TOUR ==========
    public TourResponse toTourResponse(Tour tour) { return tourMapper.toTourResponse(tour); }
    public List<TourResponse> toTourResponseList(List<Tour> tours) { return tourMapper.toTourResponseList(tours); }
    public TicketResponse toTicketResponse(Ticket ticket) { return tourMapper.toTicketResponse(ticket); }
    public List<TicketResponse> toTicketResponseList(List<Ticket> tickets) { return tourMapper.toTicketResponseList(tickets); }
    public ReservationResponse toReservationResponse(TourReservation res) { return tourMapper.toReservationResponse(res); }
    public List<ReservationResponse> toReservationResponseList(List<TourReservation> reservations) { return tourMapper.toReservationResponseList(reservations); }
    public TransportationResponse toTransportationResponse(Transportation t) { return tourMapper.toTransportationResponse(t); }
    public List<TransportationResponse> toTransportationResponseList(List<Transportation> list) { return tourMapper.toTransportationResponseList(list); }
    public SeatResponse toSeatResponse(Seat seat) { return tourMapper.toSeatResponse(seat); }
    public List<SeatResponse> toSeatResponseList(List<Seat> seats) { return tourMapper.toSeatResponseList(seats); }
    public MealPlanResponse toMealPlanResponse(MealPlan mp) { return tourMapper.toMealPlanResponse(mp); }
    public List<MealPlanResponse> toMealPlanResponseList(List<MealPlan> meals) { return tourMapper.toMealPlanResponseList(meals); }

    // ========== PAYMENT ==========
    public PaymentResponse toPaymentResponse(Payment p) { return systemMapper.toPaymentResponse(p); }
    public List<PaymentResponse> toPaymentResponseList(List<Payment> payments) { return systemMapper.toPaymentResponseList(payments); }
    public TransactionResponse toTransactionResponse(Transaction t) { return systemMapper.toTransactionResponse(t); }
    public List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions) { return systemMapper.toTransactionResponseList(transactions); }

    // ========== GEOGRAPHY ==========
    public CountryResponse toCountryResponse(Country c) { return systemMapper.toCountryResponse(c); }
    public List<CountryResponse> toCountryResponseList(List<Country> countries) { return systemMapper.toCountryResponseList(countries); }
    public CityResponse toCityResponse(City c) { return systemMapper.toCityResponse(c); }
    public List<CityResponse> toCityResponseList(List<City> cities) { return systemMapper.toCityResponseList(cities); }
    public PortResponse toPortResponse(Port p) { return systemMapper.toPortResponse(p); }
    public List<PortResponse> toPortResponseList(List<Port> ports) { return systemMapper.toPortResponseList(ports); }

    // ========== NOTIFICATION ==========
    public NotificationResponse toNotificationResponse(Notification n) { return systemMapper.toNotificationResponse(n); }
    public List<NotificationResponse> toNotificationResponseList(List<Notification> notifications) { return systemMapper.toNotificationResponseList(notifications); }
    public NotificationCounts toNotificationCounts(long unreadCount) { return systemMapper.toNotificationCounts(unreadCount); }

    // ========== SUPPORT ==========
    public UserRequestResponse toUserRequestResponse(UserRequest r) { return systemMapper.toUserRequestResponse(r); }
    public List<UserRequestResponse> toUserRequestResponseList(List<UserRequest> requests) { return systemMapper.toUserRequestResponseList(requests); }
    public CommentResponse toCommentResponse(GenericComment c) { return systemMapper.toCommentResponse(c); }
    public EventLogResponse toEventLogResponse(GenericEventLog e) { return systemMapper.toEventLogResponse(e); }

    // ========== ACCOUNT ==========
    public AccountResponse toAccountResponse(Account a) { return systemMapper.toAccountResponse(a); }
}
