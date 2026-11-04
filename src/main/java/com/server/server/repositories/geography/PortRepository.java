package com.server.server.repositories.geography;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.server.enums.common.GenericStatus;
import com.server.server.models.geography.Port;
public interface PortRepository extends JpaRepository<Port, Integer> {
    List<Port> findByStatus(GenericStatus status);
}
