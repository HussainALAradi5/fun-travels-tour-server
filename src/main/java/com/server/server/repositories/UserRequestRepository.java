package com.server.server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.server.server.models.UserRequest;

public interface UserRequestRepository
        extends JpaRepository<UserRequest, Integer>, JpaSpecificationExecutor<UserRequest> {
}
