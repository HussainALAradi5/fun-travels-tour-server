package com.server.server.services.tourmanagement;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.server.enums.common.GenericStatus;
import com.server.server.dto.tourmanagement.TourFilterRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.services.common.GenericFilterService;
import java.util.Set;
import java.util.Map;
import com.server.server.models.user.User;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.utilities.pagination.PaginationUtils;
import com.server.server.utilities.filters.FilterUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.exceptions.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

/** Read side of tours: the public catalog, lists and search, and who may see which tour. */
@Service
@RequiredArgsConstructor
public class TourCatalogService extends GenericFilterService<Tour> {

    private final TourService tourService;
    private final TourRepository tourRepository;
    private final AccessPolicyService accessPolicy;
    private final TicketRepository ticketRepository;

    /** Admins and agency staff list every tour (staff within their agency); everyone else only bookable ones. */
    @Transactional(readOnly = true)
    public PageResponse<Tour> getAll(Integer page, Integer size, String sortBy, String sortDir) {
        return PageResponse.from(tourRepository.findAll(visibleTours(accessPolicy.currentUserOrNull()),
                PaginationUtils.pageable(page, size, sortBy, sortDir, "startDate",
                        Set.of("id", "tourNumber", "title", "startDate", "endDate", "totalPrice"))));
    }

    /**
     * One tour by id for the public endpoint. Bookable (ACTIVE) tours are public; others are shown to
     * administrators, the owning agency's staff, and customers holding a ticket for it. Anyone else gets 404,
     * so pending or cancelled tours do not leak.
     */
    @Transactional(readOnly = true)
    public Tour getVisible(@NonNull Integer id) {
        Tour tour = tourService.getById(id);
        if (tour.getStatus() == GenericStatus.ACTIVE) {
            return tour;
        }
        User current = accessPolicy.currentUserOrNull();
        boolean allowed = current != null && (AccessPolicyService.isAdmin(current)
                || (AccessPolicyService.isAgencyStaff(current) && current.getAgency() != null && tour.getAgency() != null
                        && Objects.equals(current.getAgency().getId(), tour.getAgency().getId()))
                || ticketRepository.existsByTour_IdAndCustomer_Id(id, current.getId()));
        if (!allowed) {
            throw new ResourceNotFoundException("Tour", id);
        }
        return tour;
    }

    /** Admins see all tours, agency staff their agency's, everyone else only ACTIVE (bookable) tours. */
    private Specification<Tour> visibleTours(User current) {
        if (current != null && AccessPolicyService.isAdmin(current)) {
            return (r, q, cb) -> cb.conjunction();
        }
        Integer scope = current == null ? null : AccessPolicyService.agencyScope(current);
        if (scope != null) {
            return (r, q, cb) -> cb.equal(r.get("agency").get("id"), scope);
        }
        return (r, q, cb) -> cb.equal(r.get("status"), GenericStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public PageResponse<Tour> getCatalogTours(Integer startCountryId, Integer endCountryId, LocalDate start,
            LocalDate end, Integer page, Integer size) {
        return PageResponse.from(tourRepository.findToursForCatalog(startCountryId, endCountryId, start, end,
                PaginationUtils.pageable(page, size, "startDate", "asc", "startDate", Set.of("startDate"))));
    }

    @Transactional(readOnly = true)
    public PageResponse<Tour> filter(TourFilterRequest filter) {

        // Agency staff are limited to their agency; managers and employees also to their branch.
        User currentUser = accessPolicy.currentUserOrNull();
        Integer agencyScope = currentUser == null ? null : AccessPolicyService.agencyScope(currentUser);
        if (agencyScope != null) {
            filter.setAgencyId(agencyScope.longValue());
            if (AccessPolicyService.isBranchStaff(currentUser)) {
                filter.setBranchId(currentUser.getAgencyBranch() == null ? null : currentUser.getAgencyBranch().getId().longValue());
            }
        }

        Specification<Tour> spec = visibleTours(currentUser).and(hasStatus(filter.getStatus()))
                .and(hasMinSlots(filter.getMinSlots()))
                .and(FilterUtils.localDateRange("startDate", filter.getStartDate(), filter.getEndDate()))
                .and(hasAgency(filter.getAgencyId()))
                .and(hasBranch(filter.getBranchId()))
                .and(hasPriceBetween(filter.getMinPrice(), filter.getMaxPrice()))
                .and(hasCity(filter.getCityId()))
                .and(hasCountry(filter.getCountryId()))
                .and(hasCreatedBy(filter.getCreatedById()))
                .and(matchesSearch(filter.getSearch()));

        return executeFilter(tourRepository, spec, filter, "startDate",
                Set.of("id", "tourNumber", "title", "startDate", "endDate", "totalPrice", "availableSlots", "agency.name", "agencyBranch.name"),
                Map.of("agency", "agency.name", "branch", "agencyBranch.name"));
    }

    private Specification<Tour> hasBranch(Long b) {
        return (r, q, cb) -> b == null ? cb.conjunction() : cb.equal(r.get("agencyBranch").get("id"), b);
    }

    private Specification<Tour> hasStatus(GenericStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<Tour> hasMinSlots(Integer m) {
        return (r, q, cb) -> m == null ? cb.conjunction() : cb.greaterThanOrEqualTo(r.get("availableSlots"), m);
    }

    private Specification<Tour> hasAgency(Long a) {
        return (r, q, cb) -> a == null ? cb.conjunction() : cb.equal(r.get("agency").get("id"), a);
    }

    private Specification<Tour> hasPriceBetween(Double min, Double max) {
        return (r, q, cb) -> {
            if (min != null && max != null)
                return cb.between(r.get("totalPrice"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(r.get("totalPrice"), min);
            if (max != null)
                return cb.lessThanOrEqualTo(r.get("totalPrice"), max);
            return cb.conjunction();
        };
    }

    private Specification<Tour> hasCity(Integer cityId) {
        return (r, q, cb) -> {
            if (cityId == null)
                return cb.conjunction();
            return cb.or(cb.equal(r.get("startCity").get("id"), cityId), cb.equal(r.get("endCity").get("id"), cityId));
        };
    }

    private Specification<Tour> hasCountry(Integer countryId) {
        return (r, q, cb) -> {
            if (countryId == null)
                return cb.conjunction();
            Join<Object, Object> destinations = r.join("destinationCountries", JoinType.LEFT);
            return cb.or(
                    cb.equal(r.get("startCountry").get("id"), countryId),
                    cb.equal(r.get("endCountry").get("id"), countryId),
                    cb.equal(destinations.get("id"), countryId));
        };
    }

    private Specification<Tour> hasCreatedBy(Integer createdById) {
        return (r, q, cb) -> createdById == null ? cb.conjunction()
                : cb.equal(r.get("createdBy").get("id"), createdById);
    }

    private Specification<Tour> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String term = normalizeSearch(search);
            return cb.or(
                    cb.like(cb.lower(root.get("tourNumber")), term),
                    cb.like(cb.lower(root.get("title")), term),
                    cb.like(cb.lower(root.get("description")), term));
        };
    }
}
