package com.banca.gestionale_banca.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserStatsResponse {
    private long activeUsers;
}
