package com.server.server.services.report;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.constants.ReportRules;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.payment.TransactionFilterRequest;
import com.server.server.dto.report.ReportRequest;
import com.server.server.dto.tourmanagement.TicketFilterRequest;
import com.server.server.dto.tourmanagement.TourFilterRequest;
import com.server.server.dto.tourmanagement.TransportationFilterRequest;
import com.server.server.dto.common.GenericFilterRequest;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.user.User;
import com.server.server.services.agency.AgencyBranchService;
import com.server.server.services.agency.AgencyService;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.payment.TransactionService;
import com.server.server.services.tourmanagement.MealPlanService;
import com.server.server.services.tourmanagement.TicketService;
import com.server.server.services.tourmanagement.TourCatalogService;
import com.server.server.services.tourmanagement.TourService;
import com.server.server.services.tourmanagement.TransportationService;
import com.server.server.services.user.UserService;
import com.server.server.utilities.report.ReportColumn;
import com.server.server.utilities.report.ReportColumns;
import com.server.server.utilities.report.ReportTable;

import lombok.RequiredArgsConstructor;

/**
 * Loads a report's rows through the domain services, so every scoping rule (own records for customers,
 * own agency for staff) applies exactly as on the list pages. Runs in one read-only transaction and
 * returns plain values, so rendering needs no database access.
 */
@Service
@RequiredArgsConstructor
public class ReportDataService {

    private final TourService tourService;
    private final TourCatalogService tourCatalogService;
    private final TicketService ticketService;
    private final TransportationService transportationService;
    private final MealPlanService mealPlanService;
    private final TransactionService transactionService;
    private final UserService userService;
    private final AgencyService agencyService;
    private final AgencyBranchService branchService;
    private final AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public ReportTable load(ReportRequest request, User actor) {
        String title = request.type().title();
        String author = actor.getName();
        return switch (request.type()) {
            case TOUR_CATALOG -> table(title, author, ReportColumns.TOURS,
                    page -> tourCatalogService.getCatalogTours(null, null, null, null, page, ReportRules.LOAD_PAGE_SIZE));
            case TOURS -> table(title, author, ReportColumns.TOURS, page -> tourCatalogService.filter(paged(new TourFilterRequest(), page)));
            case TICKETS -> table(title, author, ReportColumns.TICKETS, page -> ticketService.filter(paged(new TicketFilterRequest(), page)));
            case PASSENGER_MANIFEST -> {
                var tour = tourService.getById(require(request.tourId(), "tourId"));
                accessPolicy.requireAgencyAccess(actor, tour.getAgency());
                TicketFilterRequest filter = new TicketFilterRequest();
                filter.setTourId(tour.getId());
                yield table(tour.getTitle() + " - " + title, author, ReportColumns.MANIFEST,
                        page -> ticketService.filter(paged(filter, page)));
            }
            case TRANSPORTATIONS -> table(title, author, ReportColumns.TRANSPORTATIONS,
                    page -> transportationService.filter(paged(new TransportationFilterRequest(), page)));
            case MEALS -> table(title, author, ReportColumns.MEALS, page -> mealPlanService.findAll(page, ReportRules.LOAD_PAGE_SIZE, "asc"));
            case TRANSACTIONS -> table(title, author, ReportColumns.TRANSACTIONS,
                    page -> transactionService.filterTransactions(paged(new TransactionFilterRequest(), page)));
            case USERS -> ReportTable.of(title, author, ReportColumns.USERS, userService.getUsersByType(request.role()), false);
            case AGENCY_BRANCHES -> {
                var agency = agencyService.getAgencyById(require(request.agencyId(), "agencyId"));
                accessPolicy.requireAgencyAccess(actor, agency);
                yield ReportTable.of(agency.getAgencyName() + " - " + title, author, ReportColumns.BRANCHES,
                        branchService.getBranchesByAgency(agency.getId()), false);
            }
            case AGENCY_EMPLOYEES -> {
                var agency = agencyService.getAgencyById(require(request.agencyId(), "agencyId"));
                yield ReportTable.of(agency.getAgencyName() + " - " + title, author, ReportColumns.USERS,
                        userService.getAgencyUsers(agency.getId(), request.role()), false);
            }
        };
    }

    /** Loads pages until the last one or the report row limit. */
    private static <T> ReportTable table(String title, String author, List<ReportColumn<T>> columns, IntFunction<PageResponse<T>> loader) {
        List<T> records = new ArrayList<>();
        int page = 0;
        PageResponse<T> current;
        do {
            current = loader.apply(page++);
            records.addAll(current.content());
        } while (page < current.totalPages() && records.size() < ReportRules.MAX_ROWS);
        boolean truncated = records.size() > ReportRules.MAX_ROWS || page < current.totalPages();
        return ReportTable.of(title, author, columns, records.subList(0, Math.min(records.size(), ReportRules.MAX_ROWS)), truncated);
    }

    private static <F extends GenericFilterRequest> F paged(F filter, int page) {
        filter.setPage(page);
        filter.setSize(ReportRules.LOAD_PAGE_SIZE);
        return filter;
    }

    private static Integer require(Integer id, String name) {
        if (id == null) {
            throw new WorkflowException("REPORT_PARAMETER_REQUIRED", "This report needs " + name + ".");
        }
        return id;
    }
}
