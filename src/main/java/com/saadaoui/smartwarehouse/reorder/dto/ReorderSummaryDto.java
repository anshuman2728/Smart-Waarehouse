package com.saadaoui.smartwarehouse.reorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderSummaryDto {
    private long totalEvaluated;
    private long totalFlagged;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private BigDecimal totalEstimatedRestockCost;
}
