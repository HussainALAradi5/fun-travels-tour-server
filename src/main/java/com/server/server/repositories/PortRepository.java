package com.server.server.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.GenericStatus;
import com.server.server.models.Port;

public interface PortRepository extends JpaRepository<Port, Integer> {
    List<Port> findByStatus(GenericStatus status);
}
