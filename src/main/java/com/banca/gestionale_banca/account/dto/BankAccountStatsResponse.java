package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankAccountStatsResponse {
    private long pendingApprovals;
    private BigDecimal totalManagedAssets;
}
