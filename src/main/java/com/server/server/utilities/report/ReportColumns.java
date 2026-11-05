package com.server.server.utilities.report;

import java.util.List;
import java.util.function.Function;

import com.server.server.dto.tourmanagement.MealPlanResponse;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.models.payment.Transaction;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.models.user.User;

/** The columns of each report, in order. Values are read inside the loading transaction. */
public final class ReportColumns {

    private ReportColumns() {
    }

    /** Null-safe access to a related record's value. */
    private static <T, R> Object nested(T parent, Function<T, R> child, Function<R, Object> value) {
        R related = parent == null ? null : child.apply(parent);
        return related == null ? null : value.apply(related);
    }

    public static final List<ReportColumn<Tour>> TOURS = List.of(
            ReportColumn.of("Tour Number", Tour::getTourNumber),
            ReportColumn.of("Title", Tour::getTitle),
            ReportColumn.of("Start Date", Tour::getStartDate),
            ReportColumn.of("End Date", Tour::getEndDate),
            ReportColumn.of("Days", Tour::getNumberOfDays),
            ReportColumn.of("Route", t -> nested(t, Tour::getStartCity, c -> c.getName()) + " - " + nested(t, Tour::getEndCity, c -> c.getName())),
            ReportColumn.of("Status", Tour::getStatus),
            ReportColumn.of("Price", Tour::getTotalPrice),
            ReportColumn.of("Capacity", Tour::getMaxCapacity),
            ReportColumn.of("Available", Tour::getAvailableSlots),
            ReportColumn.of("Vehicle", t -> nested(t, Tour::getTransportation, v -> v.getTransportationNumber())),
            ReportColumn.of("Agency", t -> nested(t, Tour::getAgency, a -> a.getAgencyName())));

    public static final List<ReportColumn<Ticket>> TICKETS = List.of(
            ReportColumn.of("Ticket Number", Ticket::getTicketNumber),
            ReportColumn.of("Passenger", t -> nested(t, Ticket::getCustomer, User::getName)),
            ReportColumn.of("Tour", t -> nested(t, Ticket::getTour, Tour::getTitle)),
            ReportColumn.of("Travel Date", t -> nested(t, Ticket::getTour, Tour::getStartDate)),
            ReportColumn.of("Seat", t -> nested(t, Ticket::getAssignedSeat, s -> s.getSeatCode())),
            ReportColumn.of("Status", Ticket::getTicketStatus),
            ReportColumn.of("Paid", Ticket::isPaid),
            ReportColumn.of("Total", Ticket::getTotalPrice),
            ReportColumn.of("Booked On", Ticket::getBookingDate));

    public static final List<ReportColumn<Ticket>> MANIFEST = List.of(
            ReportColumn.of("Ticket Number", Ticket::getTicketNumber),
            ReportColumn.of("Passenger", t -> nested(t, Ticket::getCustomer, User::getName)),
            ReportColumn.of("Mobile", t -> nested(t, Ticket::getCustomer, User::getMobileNumber)),
            ReportColumn.of("Seat", t -> nested(t, Ticket::getAssignedSeat, s -> s.getSeatCode())),
            ReportColumn.of("Meal Plan", Ticket::isHasMealPlan),
            ReportColumn.of("Status", Ticket::getTicketStatus),
            ReportColumn.of("Paid", Ticket::isPaid),
            ReportColumn.of("Total", Ticket::getTotalPrice));

    public static final List<ReportColumn<Transportation>> TRANSPORTATIONS = List.of(
            ReportColumn.of("Code", Transportation::getCode),
            ReportColumn.of("Registration", Transportation::getTransportationNumber),
            ReportColumn.of("Type", Transportation::getType),
            ReportColumn.of("Provider", Transportation::getProviderName),
            ReportColumn.of("Capacity", Transportation::getTotalCapacity),
            ReportColumn.of("Unit Status", Transportation::getUnitStatus),
            ReportColumn.of("Record Status", Transportation::getStatus),
            ReportColumn.of("Agency", v -> nested(v, Transportation::getAgency, a -> a.getAgencyName())),
            ReportColumn.of("Branch", v -> nested(v, Transportation::getAgencyBranch, AgencyBranch::getBranchName)));

    public static final List<ReportColumn<MealPlan>> MEALS = List.of(
            ReportColumn.of("Meal", MealPlan::getMealName),
            ReportColumn.of("Price", MealPlan::getMealPrice),
            ReportColumn.of("Dietary", m -> String.join(", ", MealPlanResponse.from(m).dietaryTypes().stream().map(Enum::name).toList())),
            ReportColumn.of("Spice", MealPlan::getSpiceLevel),
            ReportColumn.of("Status", MealPlan::getStatus),
            ReportColumn.of("Agency", m -> m.getAgency() == null ? "Shared" : m.getAgency().getAgencyName()),
            ReportColumn.of("Description", MealPlan::getMealDescription));

    public static final List<ReportColumn<Transaction>> TRANSACTIONS = List.of(
            ReportColumn.of("Reference", Transaction::getCode),
            ReportColumn.of("Date", Transaction::getTimestamp),
            ReportColumn.of("Type", Transaction::getType),
            ReportColumn.of("Amount", Transaction::getAmount),
            ReportColumn.of("Account", t -> nested(t, Transaction::getAccount, a -> a.getAccountNumber())),
            ReportColumn.of("Description", Transaction::getDescription));

    public static final List<ReportColumn<User>> USERS = List.of(
            ReportColumn.of("Name", User::getName),
            ReportColumn.of("Username", User::getUserName),
            ReportColumn.of("Email", User::getEmail),
            ReportColumn.of("Mobile", User::getMobileNumber),
            ReportColumn.of("Gender", User::getGender),
            ReportColumn.of("Role", User::getUserType),
            ReportColumn.of("Agency", u -> nested(u, User::getAgency, a -> a.getAgencyName())),
            ReportColumn.of("Branch", u -> nested(u, User::getAgencyBranch, AgencyBranch::getBranchName)),
            ReportColumn.of("Active", User::isActive));

    public static final List<ReportColumn<AgencyBranch>> BRANCHES = List.of(
            ReportColumn.of("Branch", AgencyBranch::getBranchName),
            ReportColumn.of("Address", AgencyBranch::getBranchAddress),
            ReportColumn.of("Contact", AgencyBranch::getContactNumber),
            ReportColumn.of("Manager", b -> nested(b, AgencyBranch::getBranchManager, User::getName)),
            ReportColumn.of("Active", AgencyBranch::isActive));
}
