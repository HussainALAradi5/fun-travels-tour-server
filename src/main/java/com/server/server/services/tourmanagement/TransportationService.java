package com.server.server.services.tourmanagement;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.dto.filter.TransportationFilterRequest;
import com.server.server.dto.importing.ImportResult;
import com.server.server.dto.tour.TransportationCreateRequest;
import com.server.server.dto.PageResponse;
import com.server.server.utilities.PaginationUtils;
import com.server.server.utilities.ExcelImportUtils;
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public PageResponse<Transportation> getAll(Integer page, Integer size, String sortBy, String sortDir) {
        return PageResponse.from(repository.findAll(PaginationUtils.pageable(page, size, sortBy, sortDir,
                "transportationNumber", Set.of("id", "transportationNumber", "code", "providerName", "type", "unitStatus"))));
    }

    @Transactional(readOnly = true)
    public Transportation getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findWithSeatsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transportation unit not found."));
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
        List<Map<String, String>> rows = ExcelImportUtils.readRows(input);
        List<String> errors = new ArrayList<>();
        int imported = 0;
        for (Map<String, String> row : rows) {
            try {
                create(toRequest(row));
                imported++;
            } catch (RuntimeException exception) {
                errors.add("Row " + row.get("_row") + ": " + exception.getMessage());
            }
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    private Transportation toEntity(TransportationCreateRequest request) {
        Agency agency = agencyRepository.findById(request.getAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency", request.getAgencyId()));
        AgencyBranch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agency branch", request.getBranchId()));
            if (branch.getAgency() == null || !branch.getAgency().getId().equals(agency.getId())) {
                throw new IllegalArgumentException("The selected branch does not belong to the selected agency.");
            }
        }
        Transportation transportation = new Transportation();
        transportation.setTransportationNumber(request.getTransportationNumber().trim());
        transportation.setCode(request.getCode().trim());
        transportation.setType(request.getType());
        transportation.setProviderName(request.getProviderName().trim());
        transportation.setTotalCapacity(request.getTotalCapacity());
        transportation.setAgency(agency);
        transportation.setAgencyBranch(branch);
        transportation.setSeatConfig(request.getSeatConfig());
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
        TransportationCreateRequest request = new TransportationCreateRequest();
        request.setTransportationNumber(required(row, "transportationnumber"));
        request.setCode(required(row, "code"));
        request.setType(parseEnum(TransportationType.class, required(row, "type"), "type"));
        request.setProviderName(required(row, "providername"));
        request.setTotalCapacity(positiveInteger(row, "totalcapacity"));
        request.setAgencyId(positiveInteger(row, "agencyid"));
        request.setBranchId(optionalInteger(row, "branchid"));
        Map<String, Integer> seats = new LinkedHashMap<>();
        putSeatCount(seats, "PREMIUM_RECLINER", row.get("premiumseats"));
        putSeatCount(seats, "WHEELCHAIR_ACCESSIBLE", row.get("accessibleseats"));
        putSeatCount(seats, "KIDS_CHAIR", row.get("kidsseats"));
        request.setSeatConfig(seats);
        return request;
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
    public Transportation update(@NonNull Integer id, Transportation incomingData) {
        Objects.requireNonNull(id, "id must not be null");
        Transportation existing = getById(id);

        // Efficient update ignoring managed relationships and status fields
        BeanUtils.copyProperties(incomingData, existing,
                "id", "status", "unitStatus", "seats", "remainingSeats", "calculatedAvailable",
                "agency", "agencyBranch", "tours");

        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public Transportation updateStatus(@NonNull Integer id, TransportationStatus newUnitStatus) {
        Objects.requireNonNull(id, "id must not be null");
        Transportation transport = getById(id);
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
    public PageResponse<Transportation> filter(TransportationFilterRequest filter) {
        String search = filter.getKeyword() != null ? filter.getKeyword() : filter.getSearch();
        Specification<Transportation> spec = hasType(filter.getType())
                .and(hasStatus(filter.getStatus())).and(hasUnitStatus(filter.getUnitStatus()))
                .and(hasProvider(search));
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

    private Specification<Transportation> hasProvider(String p) {
        return (r, q, cb) -> (p == null || p.isBlank()) ? cb.conjunction()
                : cb.like(cb.lower(r.get("providerName")), "%" + p.toLowerCase() + "%");
    }
}
