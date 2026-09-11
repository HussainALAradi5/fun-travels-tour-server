package com.server.server.dto.filter;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import com.server.server.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransactionFilterRequest extends GenericFilterRequest {
    private Integer userId;
    private TransactionType type;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    private Long agencyId;
    private Long branchId;
}
