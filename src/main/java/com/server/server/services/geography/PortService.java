package com.server.server.services.geography;

import com.server.server.exceptions.ResourceNotFoundException;

import com.server.server.repositories.geography.CityRepository;

import com.server.server.repositories.geography.CountryRepository;

import com.server.server.models.geography.City;

import com.server.server.models.geography.Country;

import com.server.server.dto.geography.PortCreateRequest;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.common.GenericStatus;
import com.server.server.models.geography.Port;
import com.server.server.repositories.geography.PortRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortService {
    private final PortRepository portRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<Port> getAllActivePorts() {
        return portRepository.findByStatus(GenericStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Port getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return portRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Port not found"));
    }

    @Transactional
    public Port createPort(PortCreateRequest request) {
        Country country = countryRepository.findById(request.getCountry().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Country", request.getCountry().getId()));
        City city = cityRepository.findById(request.getCity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("City", request.getCity().getId()));
        if (!Objects.equals(city.getCountry().getId(), country.getId())) {
            throw new IllegalArgumentException("The selected city does not belong to the selected country.");
        }
        Port port = new Port();
        port.setPortName(request.getPortName().trim());
        port.setPortCode(request.getPortCode());
        port.setPortType(request.getPortType());
        port.setCity(city);
        port.setCountry(country);
        port.setLatitude(request.getLatitude());
        port.setLongitude(request.getLongitude());
        port.setStatus(GenericStatus.ACTIVE);
        return portRepository.save(port);
    }

    @Transactional
    public Port updateStatus(@NonNull Integer id, GenericStatus status) {
        Objects.requireNonNull(id, "id must not be null");
        Port port = getById(id);
        port.setStatus(status);
        return portRepository.save(port);
    }
}
