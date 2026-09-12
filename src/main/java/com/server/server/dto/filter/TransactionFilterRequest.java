package com.server.server.dto.filter;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransactionFilterRequest extends GenericFilterRequest {
    private Integer userId;
    private TransactionType type;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Long agencyId;
    private Long branchId;
}
