package com.server.server.services.filter;

import java.util.Map;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.GenericFilterRequest;
import com.server.server.utilities.PaginationUtils;

public abstract class GenericFilterService<T> {

    protected String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
    }

    protected PageResponse<T> executeFilter(
            JpaSpecificationExecutor<T> repository,
            Specification<T> specification,
            GenericFilterRequest filter,
            String defaultSort,
            Set<String> allowedSorts) {
        return executeFilter(repository, specification, filter, defaultSort, allowedSorts, Map.of());
    }

    protected PageResponse<T> executeFilter(
            JpaSpecificationExecutor<T> repository,
            Specification<T> specification,
            GenericFilterRequest filter,
            String defaultSort,
            Set<String> allowedSorts,
            Map<String, String> sortAliases) {
        return PageResponse.from(repository.findAll(specification,
                PaginationUtils.pageable(filter, defaultSort, allowedSorts, sortAliases)));
    }
}
