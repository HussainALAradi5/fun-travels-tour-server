package com.server.server.dto.support;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    private Integer id;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;
    private String type;
    private String status;
}
