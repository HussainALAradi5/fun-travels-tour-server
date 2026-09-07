package com.server.server.services;

import java.util.List;

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
    public Port getById(Integer id) {
        return portRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Port not found"));
    }

    @Transactional
    public Port createPort(Port port) {
        port.setStatus(GenericStatus.ACTIVE);
        return portRepository.save(port);
    }

    @Transactional
    public Port updateStatus(Integer id, GenericStatus status) {
        Port port = getById(id);
        port.setStatus(status);
        return portRepository.save(port);
    }
}