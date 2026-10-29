package com.saadaoui.smartwarehouse.reorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleInputsDto {
    private int currentStock;
    private int safetyStock;
    private double avgDailyDemand;
    private int leadTimeDays;
    private int reorderPoint;
    private Double daysOfCover;
    private String reorderPointFormula;
    private String daysOfCoverFormula;
    private String recommendedQuantityFormula;
}
