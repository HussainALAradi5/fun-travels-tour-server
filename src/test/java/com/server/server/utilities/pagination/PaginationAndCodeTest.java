package com.server.server.utilities.pagination;

import com.server.server.utilities.codes.CodeGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.server.server.models.tourmanagement.Tour;

class PaginationAndCodeTest {

    @Test
    void clampsPageSizeAndNegativePage() {
        Pageable pageable = PaginationUtils.pageable(-3, 10_000, null, "asc", "id", Set.of("id"));
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(PaginationUtils.MAX_SIZE);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsSortFieldsOutsideTheAllowList() {
        assertThatThrownBy(() -> PaginationUtils.pageable(0, 10, "password", "asc", "id", Set.of("id")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesSortAliases() {
        Pageable pageable = PaginationUtils.pageable(0, 10, "startDate", "desc", "id",
                Set.of("id", "tour.startDate"), Map.of("startDate", "tour.startDate"));
        assertThat(pageable.getSort().getOrderFor("tour.startDate")).isNotNull();
    }

    @Test
    void generatesPrefixedUniqueCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(CodeGenerator.generate("tkt"));
        }
        assertThat(codes).hasSize(1000).allMatch(code -> code.matches("TKT-[2-9A-HJ-NP-Z]{10}"));
        assertThat(CodeGenerator.prefixFor(Tour.class)).isEqualTo("TOU");
    }
}
