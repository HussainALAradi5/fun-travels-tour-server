package com.server.server.utilities.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.server.server.dto.tour.TourResponse;
import com.server.server.dto.tour.TicketResponse;
import com.server.server.dto.tour.ReservationResponse;
import com.server.server.dto.tour.TransportationResponse;
import com.server.server.dto.tour.SeatResponse;
import com.server.server.dto.tour.MealPlanResponse;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.MealPlan;

@Component
public class TourMapper {

    public TourResponse toTourResponse(Tour tour) {
        if (tour == null) return null;
        TourResponse dto = new TourResponse();
        dto.setId(tour.getId());
        dto.setTourNumber(tour.getTourNumber());
        dto.setTitle(tour.getTitle());
        dto.setDescription(tour.getDescription());
        dto.setBasePrice(tour.getBasePrice());
        dto.setDiscountPrice(tour.getDiscountPrice());
        dto.setTotalPrice(tour.getTotalPrice());
        dto.setNumberOfDays(tour.getNumberOfDays());
        dto.setStartDate(tour.getStartDate());
        dto.setEndDate(tour.getEndDate());
        dto.setMaxCapacity(tour.getMaxCapacity());
        dto.setAvailableSlots(tour.getAvailableSlots());
        dto.setStatus(tour.getStatus());
        dto.setHasTransportation(tour.isHasTransportation());
        if (tour.getStartCountry() != null) {
            dto.setStartCountry(new TourResponse.CountrySummary(
                    tour.getStartCountry().getId(), tour.getStartCountry().getFamousName()));
        }
        if (tour.getEndCountry() != null) {
            dto.setEndCountry(new TourResponse.CountrySummary(
                    tour.getEndCountry().getId(), tour.getEndCountry().getFamousName()));
        }
        if (tour.getStartCity() != null) {
            dto.setStartCity(new TourResponse.CitySummary(
                    tour.getStartCity().getId(), tour.getStartCity().getName()));
        }
        if (tour.getEndCity() != null) {
            dto.setEndCity(new TourResponse.CitySummary(
                    tour.getEndCity().getId(), tour.getEndCity().getName()));
        }
        if (tour.getAgency() != null) {
            dto.setAgency(new TourResponse.AgencySummary(
                    tour.getAgency().getId(), tour.getAgency().getAgencyName()));
        }
        if (tour.getAgencyBranch() != null) {
            dto.setAgencyBranch(new TourResponse.BranchSummary(
                    tour.getAgencyBranch().getId(), tour.getAgencyBranch().getBranchName()));
        }
        if (tour.getTransportation() != null) {
            dto.setTransportation(new TourResponse.TransportationSummary(
                    tour.getTransportation().getId(),
                    tour.getTransportation().getTransportationNumber(),
                    tour.getTransportation().getType() != null ? tour.getTransportation().getType().name() : null));
        }
        if (tour.getCreatedBy() != null) {
            dto.setCreatedBy(new TourResponse.UserSummary(
                    tour.getCreatedBy().getId(), tour.getCreatedBy().getName()));
        }
        dto.setCreatedAt(tour.getCreatedAt());
        return dto;
    }

    public List<TourResponse> toTourResponseList(List<Tour> tours) {
        return tours.stream().map(this::toTourResponse).collect(Collectors.toList());
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        if (ticket == null) return null;
        TicketResponse dto = new TicketResponse();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setBasePrice(ticket.getBasePrice());
        dto.setDiscountPrice(ticket.getDiscountPrice());
        dto.setSeatPriceModifier(ticket.getSeatPriceModifier());
        dto.setTotalPrice(ticket.getTotalPrice());
        dto.setBookingDate(ticket.getBookingDate());
        dto.setPaid(ticket.isPaid());
        dto.setTicketStatus(ticket.getTicketStatus());
        dto.setApprovalStatus(ticket.getApprovalStatus());
        dto.setHasMealPlan(ticket.isHasMealPlan());
        dto.setQrCode(ticket.getQrCode());
        dto.setBarcode(ticket.getBarcode());
        dto.setCreatedAt(ticket.getCreatedAt());
        if (ticket.getCustomer() != null) {
            dto.setCustomer(new TicketResponse.UserSummary(
                    ticket.getCustomer().getId(), ticket.getCustomer().getName()));
        }
        if (ticket.getTour() != null) {
            dto.setTour(new TicketResponse.TourSummary(
                    ticket.getTour().getId(), ticket.getTour().getTourNumber(), ticket.getTour().getTitle()));
        }
        if (ticket.getAssignedSeat() != null) {
            dto.setAssignedSeat(new TicketResponse.SeatSummary(
                    ticket.getAssignedSeat().getId(), ticket.getAssignedSeat().getSeatCode()));
        }
        return dto;
    }

    public List<TicketResponse> toTicketResponseList(List<Ticket> tickets) {
        return tickets.stream().map(this::toTicketResponse).collect(Collectors.toList());
    }

    public ReservationResponse toReservationResponse(TourReservation res) {
        if (res == null) return null;
        ReservationResponse dto = new ReservationResponse();
        dto.setId(res.getId());
        dto.setReservationNumber(res.getReservationNumber());
        dto.setRequestedSlots(res.getRequestedSlots());
        dto.setTotalPrice(res.getTotalPrice());
        dto.setStatus(res.getStatus());
        dto.setBookingDate(res.getBookingDate());
        dto.setHoldExpiresAt(res.getHoldExpiresAt());
        if (res.getTour() != null) {
            dto.setTour(new ReservationResponse.TourSummary(
                    res.getTour().getId(), res.getTour().getTourNumber(), res.getTour().getTitle()));
        }
        if (res.getUser() != null) {
            dto.setUser(new ReservationResponse.UserSummary(
                    res.getUser().getId(), res.getUser().getName()));
        }
        return dto;
    }

    public List<ReservationResponse> toReservationResponseList(List<TourReservation> reservations) {
        return reservations.stream().map(this::toReservationResponse).collect(Collectors.toList());
    }

    public TransportationResponse toTransportationResponse(Transportation t) {
        if (t == null) return null;
        TransportationResponse dto = new TransportationResponse();
        dto.setId(t.getId());
        dto.setTransportationNumber(t.getTransportationNumber());
        dto.setCode(t.getCode());
        dto.setType(t.getType());
        dto.setProviderName(t.getProviderName());
        dto.setStatus(t.getStatus());
        dto.setUnitStatus(t.getUnitStatus());
        dto.setTotalCapacity(t.getTotalCapacity());
        dto.setRemainingSeats(t.getRemainingSeats());
        dto.setCalculatedAvailable(t.getCalculatedAvailable());
        if (t.getSeats() != null) {
            dto.setSeats(t.getSeats().stream()
                    .map(s -> new TransportationResponse.SeatSummary(
                            s.getId(), s.getSeatCode(),
                            s.getChairType() != null ? s.getChairType().name() : null,
                            s.getStatus() != null ? s.getStatus().name() : null))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public List<TransportationResponse> toTransportationResponseList(List<Transportation> list) {
        return list.stream().map(this::toTransportationResponse).collect(Collectors.toList());
    }

    public SeatResponse toSeatResponse(Seat seat) {
        if (seat == null) return null;
        return new SeatResponse(seat.getId(), seat.getSeatCode(), seat.getChairType(),
                seat.getStatus(), seat.getSeatPriceModifier());
    }

    public List<SeatResponse> toSeatResponseList(List<Seat> seats) {
        return seats.stream().map(this::toSeatResponse).collect(Collectors.toList());
    }

    public MealPlanResponse toMealPlanResponse(MealPlan mp) {
        if (mp == null) return null;
        return new MealPlanResponse(mp.getId(), mp.getMealName(), mp.getMealPrice(),
                mp.getMealDescription(), mp.isVegetarian(), mp.isVegan(), mp.isGlutenFree(), mp.getStatus());
    }

    public List<MealPlanResponse> toMealPlanResponseList(List<MealPlan> meals) {
        return meals.stream().map(this::toMealPlanResponse).collect(Collectors.toList());
    }
}
