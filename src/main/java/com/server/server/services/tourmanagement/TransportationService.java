package com.server.server.services.tourmanagement;

import com.server.server.dto.tourmanagement.TransportationUpdateRequest;

import com.server.server.exceptions.DuplicateResourceException;

import com.server.server.services.auth.AccessPolicyService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.dto.tourmanagement.TransportationFilterRequest;
import com.server.server.dto.common.ImportResult;
import com.server.server.dto.tourmanagement.TransportationCreateRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.utilities.PaginationUtils;
import com.server.server.services.common.ExcelImportService;
import java.util.Set;
import com.server.server.exceptions.WorkflowException;
import com.server.server.exceptions.ResourceNotFoundException;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.repositories.agency.AgencyRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.tourmanagement.TransportationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportationService {
    private final TransportationRepository repository;
    private final AgencyRepository agencyRepository;
    private final AgencyBranchRepository branchRepository;
    private final ExcelImportService excelImportService;
    private final AccessPolicyService accessPolicy;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public PageResponse<Transportation> getAll(Integer page, Integer size, String sortBy, String sortDir) {
        return PageResponse.from(repository.findAll(inViewerScope(), PaginationUtils.pageable(page, size, sortBy, sortDir,
                "transportationNumber", Set.of("id", "transportationNumber", "code", "providerName", "type", "unitStatus"))));
    }

    @Transactional(readOnly = true)
    public Transportation getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findWithSeatsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transportation", id));
    }

    /** A vehicle with its seats, for staff of its agency and administrators. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Transportation getVisible(@NonNull Integer id) {
        Transportation transportation = getById(id);
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(), transportation.getAgency());
        return transportation;
    }

// Inside TransportationService.java
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Transportation create(TransportationCreateRequest request) {
        Transportation transportation = toEntity(request);
        validateUniqueIdentifiers(transportation);
        transportation.setStatus(GenericStatus.ACTIVE);
        transportation.setUnitStatus(TransportationStatus.AVAILABLE);
        transportation.setRemainingSeats(transportation.getTotalCapacity());

        // THE DRY CALL: Auto-generate seats at the exact moment of creation
        if (transportation.getSeats() == null || transportation.getSeats().isEmpty()) {
            transportation.setSeats(generateSeatLayout(transportation, transportation.getSeatConfig()));
        } else {
            for (Seat seat : transportation.getSeats()) {
                seat.setTransportation(transportation);
            }
        }
        return repository.save(transportation);
    }

    public ImportResult importExcel(InputStream input) throws IOException {
        List<Map<String, String>> rows = excelImportService.readRowsByHeader(input);
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (Map<String, String> row : rows) {
            try {
                create(toRequest(row));
                imported++;
            } catch (RuntimeException exception) {
                errors.add("Row " + row.get(ExcelImportService.ROW_NUMBER_KEY) + ": " + exception.getMessage());
            }
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    private Transportation toEntity(TransportationCreateRequest request) {
        Integer scope = AccessPolicyService.agencyScope(accessPolicy.requireCurrentUser());
        Integer agencyId = scope != null ? scope : request.agencyId();
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", agencyId));
        AgencyBranch branch = null;
        if (request.branchId() != null) {
            branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agency branch", request.branchId()));
            if (branch.getAgency() == null || !branch.getAgency().getId().equals(agency.getId())) {
                throw new IllegalArgumentException("The selected branch does not belong to the selected agency.");
            }
        }
        Transportation transportation = new Transportation();
        transportation.setTransportationNumber(request.transportationNumber().trim());
        transportation.setCode(request.code().trim());
        transportation.setType(request.type());
        transportation.setProviderName(request.providerName().trim());
        transportation.setTotalCapacity(request.totalCapacity());
        transportation.setAgency(agency);
        transportation.setAgencyBranch(branch);
        transportation.setSeatConfig(request.seatConfig());
        return transportation;
    }

    private void validateUniqueIdentifiers(Transportation transportation) {
        if (repository.existsByCode(transportation.getCode())) {
            throw new IllegalArgumentException("Transportation code already exists: " + transportation.getCode());
        }
        if (repository.existsByProviderNameAndTransportationNumber(
                transportation.getProviderName(), transportation.getTransportationNumber())) {
            throw new IllegalArgumentException("This registration already exists for the selected provider.");
        }
    }

    private TransportationCreateRequest toRequest(Map<String, String> row) {
        Map<String, Integer> seats = new LinkedHashMap<>();
        putSeatCount(seats, "PREMIUM_RECLINER", row.get("premiumseats"));
        putSeatCount(seats, "WHEELCHAIR_ACCESSIBLE", row.get("accessibleseats"));
        putSeatCount(seats, "KIDS_CHAIR", row.get("kidsseats"));
        return new TransportationCreateRequest(
                required(row, "transportationnumber"),
                required(row, "code"),
                parseEnum(TransportationType.class, required(row, "type"), "type"),
                required(row, "providername"),
                positiveInteger(row, "totalcapacity"),
                positiveInteger(row, "agencyid"),
                optionalInteger(row, "branchid"),
                seats);
    }

    private String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required.");
        return value;
    }

    private Integer positiveInteger(Map<String, String> row, String key) {
        Integer value = optionalInteger(row, key);
        if (value == null || value < 1) throw new IllegalArgumentException(key + " must be a positive integer.");
        return value;
    }

    private Integer optionalInteger(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) return null;
        try {
            return new java.math.BigDecimal(value).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a whole number.");
        }
    }

    private void putSeatCount(Map<String, Integer> seats, String type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return;
        try {
            int count = new java.math.BigDecimal(rawValue).intValueExact();
            if (count < 0) throw new IllegalArgumentException(type + " seat count cannot be negative.");
            seats.put(type, count);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(type + " seat count must be a whole number.");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String field) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    // --- THE DRY HELPER METHOD ---
    private List<Seat> generateSeatLayout(Transportation transport, java.util.Map<String, Integer> config) {
        List<Seat> generated = new java.util.ArrayList<>();
        int seatNum = 1;
        int max = transport.getTotalCapacity();

        // 1. Generate Custom Chair Types if requested
        if (config != null) {
            for (java.util.Map.Entry<String, Integer> entry : config.entrySet()) {
                com.server.server.enums.tourmanagement.ChairType type = 
                    com.server.server.enums.tourmanagement.ChairType.valueOf(entry.getKey());
                int count = entry.getValue() != null ? entry.getValue() : 0;
                
                for (int i = 0; i < count; i++) {
                    if (seatNum > max) throw new WorkflowException("Configuration exceeds capacity!");
                    generated.add(buildSeat(transport, seatNum++, type));
                }
            }
        }
        
        // 2. Automatically fill the remaining capacity with STANDARD seats
        while (seatNum <= max) {
            generated.add(buildSeat(transport, seatNum++, com.server.server.enums.tourmanagement.ChairType.STANDARD));
        }
        return generated;
    }

    private Seat buildSeat(Transportation transport, int number, com.server.server.enums.tourmanagement.ChairType type) {
        Seat seat = new Seat();
        seat.setSeatCode("S" + number);
        seat.setChairType(type);
        seat.setStatus(com.server.server.enums.tourmanagement.SeatStatus.AVAILABLE);
        seat.setSeatPriceModifier(0.0);
        seat.setTransportation(transport);
        return seat;
    }
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Transportation update(@NonNull Integer id, TransportationUpdateRequest incomingData) {
        Objects.requireNonNull(id, "id must not be null");
        Transportation existing = getById(id);
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(), existing.getAgency());
        // Capacity follows the seat layout and the code is permanent, so neither is copied.
        if (incomingData.transportationNumber() != null && !incomingData.transportationNumber().isBlank()) {
            existing.setTransportationNumber(incomingData.transportationNumber().trim());
        }
        if (incomingData.providerName() != null && !incomingData.providerName().isBlank()) {
            existing.setProviderName(incomingData.providerName().trim());
        }
        if (incomingData.type() != null) {
            existing.setType(incomingData.type());
        }
        if (repository.existsByProviderNameAndTransportationNumberAndIdNot(
                existing.getProviderName(), existing.getTransportationNumber(), existing.getId())) {
            throw new DuplicateResourceException("This registration already exists for the selected provider.");
        }

        if (incomingData.seatConfig() != null) {
            applySeatLayout(existing, incomingData.seatConfig());
        }

        return repository.save(existing);
    }

    private void applySeatLayout(Transportation transportation, Map<String, Integer> requestedLayout) {
        List<Seat> seats = transportation.getSeats();
        if (seats == null || seats.isEmpty()) {
            transportation.setSeats(generateSeatLayout(transportation, requestedLayout));
            return;
        }

        if (seats.stream().anyMatch(seat -> inventoryService.isSeatInUse(seat.getId()))) {
            throw new WorkflowException("SEAT_LAYOUT_LOCKED",
                    "Seat layout cannot be changed while seats are held by active tickets.");
        }

        if (transportation.getTours() != null && transportation.getTours().stream()
                .anyMatch(tour -> tour.getStatus() == GenericStatus.APPROVED
                        || tour.getStatus() == GenericStatus.ACTIVE)) {
            throw new WorkflowException(
                    "Seat layout cannot be changed while this transportation is assigned to an approved or active tour.");
        }

        Map<com.server.server.enums.tourmanagement.ChairType, Integer> counts = new LinkedHashMap<>();
        int configuredSeats = 0;
        for (Map.Entry<String, Integer> entry : requestedLayout.entrySet()) {
            com.server.server.enums.tourmanagement.ChairType chairType;
            try {
                chairType = com.server.server.enums.tourmanagement.ChairType.valueOf(entry.getKey());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unsupported chair type: " + entry.getKey());
            }
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count < 0) throw new IllegalArgumentException("Seat counts cannot be negative.");
            if (chairType != com.server.server.enums.tourmanagement.ChairType.STANDARD) {
                counts.put(chairType, count);
                configuredSeats += count;
            }
        }

        if (configuredSeats > seats.size()) {
            throw new WorkflowException(
                    "Configured seat counts exceed the transportation capacity of " + seats.size() + ".");
        }

        List<Seat> orderedSeats = seats.stream()
                .sorted(java.util.Comparator.comparing(Seat::getId))
                .toList();
        int index = 0;
        for (Map.Entry<com.server.server.enums.tourmanagement.ChairType, Integer> entry : counts.entrySet()) {
            for (int count = 0; count < entry.getValue(); count++) {
                orderedSeats.get(index++).setChairType(entry.getKey());
            }
        }
        while (index < orderedSeats.size()) {
            orderedSeats.get(index++).setChairType(
                    com.server.server.enums.tourmanagement.ChairType.STANDARD);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Transportation updateStatus(@NonNull Integer id, TransportationStatus newUnitStatus) {
        Objects.requireNonNull(id, "id must not be null");
        Transportation transport = getById(id);
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(), transport.getAgency());
        validateStatusTransition(transport.getUnitStatus(), newUnitStatus);
        transport.setUnitStatus(newUnitStatus);
        return repository.save(transport);
    }

    private void validateStatusTransition(TransportationStatus current, TransportationStatus target) {
        if (current == TransportationStatus.FULL && target == TransportationStatus.AVAILABLE) {
            throw new WorkflowException(
                    "Cannot force a FULL unit to AVAILABLE. Seats must be freed via ticket cancellations.");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public PageResponse<Transportation> filter(TransportationFilterRequest filter) {
        String search = filter.getKeyword() != null ? filter.getKeyword() : filter.getSearch();
        Specification<Transportation> spec = inViewerScope().and(hasType(filter.getType()))
                .and(hasStatus(filter.getStatus())).and(hasUnitStatus(filter.getUnitStatus()))
                .and(matchesKeyword(search));
        return PageResponse.from(repository.findAll(spec, PaginationUtils.pageable(filter,
                "transportationNumber", Set.of("id", "transportationNumber", "code", "providerName", "type", "unitStatus"),
                java.util.Map.of())));
    }

    private Specification<Transportation> hasType(TransportationType t) {
        return (r, q, cb) -> t == null ? cb.conjunction() : cb.equal(r.get("type"), t);
    }

    private Specification<Transportation> hasStatus(GenericStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<Transportation> hasUnitStatus(TransportationStatus us) {
        return (r, q, cb) -> us == null ? cb.conjunction() : cb.equal(r.get("unitStatus"), us);
    }

    /** Keyword search over provider, internal code, and registration number. */
    private Specification<Transportation> matchesKeyword(String keyword) {
        return (r, q, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(r.get("providerName")), like), cb.like(cb.lower(r.get("code")), like),
                    cb.like(cb.lower(r.get("transportationNumber")), like));
        };
    }

    /** Agency staff see only their agency's vehicles; administrators see all. */
    private Specification<Transportation> inViewerScope() {
        Integer scope = AccessPolicyService.agencyScope(accessPolicy.requireCurrentUser());
        return (r, q, cb) -> scope == null ? cb.conjunction() : cb.equal(r.get("agency").get("id"), scope);
    }
}
