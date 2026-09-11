package com.server.server.services.filter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.server.server.dto.filter.GenericFilterRequest;

public abstract class GenericFilterService<T> {

    protected String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
    }

    protected List<T> executeFilter(
            JpaSpecificationExecutor<T> repository,
            Specification<T> specification,
            GenericFilterRequest filter,
            String defaultSort,
            Set<String> allowedSorts) {
        return executeFilter(repository, specification, filter, defaultSort, allowedSorts, Map.of());
    }

    protected List<T> executeFilter(
            JpaSpecificationExecutor<T> repository,
            Specification<T> specification,
            GenericFilterRequest filter,
            String defaultSort,
            Set<String> allowedSorts,
            Map<String, String> sortAliases) {
        String requestedSort = filter.getSortBy();
        String sortField = requestedSort == null ? defaultSort : sortAliases.getOrDefault(requestedSort, requestedSort);
        if (!allowedSorts.contains(sortField)) sortField = defaultSort;

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortField);

        if (filter.getPage() != null || filter.getSize() != null) {
            int page = Math.max(0, filter.getPage() == null ? 0 : filter.getPage());
            int size = Math.min(100, Math.max(1, filter.getSize() == null ? 20 : filter.getSize()));
            return repository.findAll(specification, PageRequest.of(page, size, sort)).getContent();
        }
        return repository.findAll(specification, sort);
    }
}
