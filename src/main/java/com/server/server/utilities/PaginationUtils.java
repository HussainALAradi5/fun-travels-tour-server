package com.server.server.utilities;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.server.server.dto.filter.GenericFilterRequest;

public final class PaginationUtils {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationUtils() {}

    public static Pageable pageable(Integer page, Integer size, String sortBy, String sortDir,
            String defaultSort, Set<String> allowedSorts) {
        return pageable(page, size, sortBy, sortDir, defaultSort, allowedSorts, Map.of());
    }

    public static Pageable pageable(GenericFilterRequest request, String defaultSort,
            Set<String> allowedSorts, Map<String, String> aliases) {
        return pageable(request.getPage(), request.getSize(), request.getSortBy(), request.getSortDir(),
                defaultSort, allowedSorts, aliases);
    }

    public static Pageable pageable(Integer page, Integer size, String sortBy, String sortDir,
            String defaultSort, Set<String> allowedSorts, Map<String, String> aliases) {
        int safePage = Math.max(DEFAULT_PAGE, page == null ? DEFAULT_PAGE : page);
        int safeSize = Math.min(MAX_SIZE, Math.max(1, size == null ? DEFAULT_SIZE : size));
        String requested = sortBy == null || sortBy.isBlank() ? defaultSort : sortBy;
        String field = aliases.getOrDefault(requested, requested);
        if (!allowedSorts.contains(field)) field = defaultSort;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}
