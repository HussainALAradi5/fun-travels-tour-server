package com.server.server.services.agency;

import com.server.server.enums.user.UserTypeEnum;

import com.server.server.exceptions.common.DuplicateResourceException;

import com.server.server.dto.agency.AgencyCreateRequest;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.models.geography.City;
import com.server.server.models.geography.Country;
import com.server.server.models.user.User;
import com.server.server.models.agency.Agency;
import com.server.server.dto.common.PageResponse;
import com.server.server.repositories.geography.CityRepository;
import com.server.server.repositories.geography.CountryRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.repositories.agency.AgencyRepository;
import com.server.server.utilities.pagination.PaginationUtils;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import com.server.server.services.auth.AccessPolicyService;

@Service
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final AccessPolicyService accessPolicy;

    /** Admins see every agency; agency staff only their own. */
    @Transactional(readOnly = true)
    public List<Agency> getAllAgencies() {
        User current = accessPolicy.requireCurrentUser();
        if (AccessPolicyService.isAdmin(current)) {
            return agencyRepository.findAll();
        }
        return current.getAgency() == null ? List.of() : List.of(getAgencyById(current.getAgency().getId()));
    }

    /** Admins search every agency; agency staff find only their own; other roles are refused. */
    @Transactional(readOnly = true)
    public PageResponse<Agency> searchAgencies(String query, Integer page, Integer size) {
        User current = accessPolicy.requireCurrentUser();
        String safeQuery = query == null ? "" : query.trim();
        var pageable = PaginationUtils.pageable(
                page, size, "agencyName", "asc", "agencyName", Set.of("agencyName"));
        if (!AccessPolicyService.isAdmin(current)) {
            Agency own = current.getAgency();
            accessPolicy.requireAgencyMember(current, own);
            boolean matches = own.getAgencyName().toLowerCase().contains(safeQuery.toLowerCase());
            List<Agency> content = matches ? List.of(own) : List.of();
            return new PageResponse<>(content, 0, content.size(), content.size(), content.isEmpty() ? 0 : 1);
        }
        return PageResponse.from(safeQuery.isEmpty()
                ? agencyRepository.findAll(pageable)
                : agencyRepository.findByAgencyNameContainingIgnoreCase(safeQuery, pageable));
    }

    /** Agency details (owner contacts included) for administrators and the agency's own staff. */
    @Transactional(readOnly = true)
    public Agency getVisibleAgency(@NonNull Integer id) {
        Agency agency = getAgencyById(id);
        accessPolicy.requireAgencyMember(accessPolicy.requireCurrentUser(), agency);
        return agency;
    }

    @Transactional(readOnly = true)
    public Agency getAgencyById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agency", id));
    }

    /** Admin-only. The optional owner must already have the OWNER role. */
    @Transactional
    public Agency createAgency(AgencyCreateRequest request) {
        Country country = request.countryId() == null ? null : countryRepository.findById(request.countryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country", request.countryId()));
        City city = request.cityId() == null ? null : cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.cityId()));
        if (city != null && country != null && !Objects.equals(city.getCountry().getId(), country.getId())) {
            throw new IllegalArgumentException("The selected city does not belong to the selected country.");
        }
        String name = request.agencyName().trim();
        if (country != null && agencyRepository.existsByAgencyNameIgnoreCaseAndCountryId(name, country.getId())) {
            throw new DuplicateResourceException("An agency named '" + name + "' already exists in this country.");
        }
        Agency agency = new Agency();
        agency.setAgencyName(name);
        agency.setAddress(request.address());
        agency.setContactNumber(request.contactNumber());
        agency.setOwnerMobileNumber(request.ownerMobileNumber());
        agency.setCountry(country);
        agency.setCity(city);
        agency.setActive(true);
        Agency saved = agencyRepository.saveAndFlush(agency);
        if (request.agencyOwnerId() != null) {
            User owner = userRepository.findById(request.agencyOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.agencyOwnerId()));
            if (owner.getUserType() != UserTypeEnum.OWNER) {
                throw new IllegalArgumentException("The selected agency owner must have the OWNER role.");
            }
            owner.setAgency(saved);
            userRepository.save(owner);
            saved.setAgencyOwner(owner);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<User> getEmployeesByAgencyId(@NonNull Integer agencyId) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        accessPolicy.requireAgencyMember(accessPolicy.requireCurrentUser(), getAgencyById(agencyId));
        return userRepository.findByAgencyIdAndIsActiveTrue(agencyId);
    }

}
