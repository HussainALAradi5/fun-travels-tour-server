package com.server.server.services;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.models.Port;
import com.server.server.repositories.PortRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortService {
    private final PortRepository portRepository;

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
    public Port createPort(Port port) {
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
