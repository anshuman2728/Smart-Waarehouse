package com.saadaoui.smartwarehouse.reorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRecommendationDto {

    private UUID productId;
    private String productName;
    private String sku;
    private UUID categoryId;
    private String categoryName;
    private UUID supplierId;
    private String supplierName;
    private String supplierEmail;
    private BigDecimal unitPrice;

    private int currentStock;
    private int safetyStock;
    private int leadTimeDays;
    private double avgDailyDemand;
    private Double daysOfCover;
    private int reorderPoint;
    private int recommendedQuantity;
    private BigDecimal estimatedCost;

    private ReorderPriority priority;
    private ReorderRule ruleApplied;
    private String reason;
    private RuleInputsDto ruleInputs;
}
