package com.saadaoui.smartwarehouse.reorder.service.impl;

import com.saadaoui.smartwarehouse.entity.Product;
import com.saadaoui.smartwarehouse.movement.repository.StockMovementRepository;
import com.saadaoui.smartwarehouse.movement.repository.StockMovementRepository.ProductOutboundProjection;
import com.saadaoui.smartwarehouse.product.repository.ProductRepository;
import com.saadaoui.smartwarehouse.reorder.dto.*;
import com.saadaoui.smartwarehouse.reorder.service.ReorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReorderServiceImpl implements ReorderService {

    public static final int DEFAULT_LOOKBACK_DAYS = 30;
    public static final int DEFAULT_LEAD_TIME_DAYS = 7;
    public static final int MIN_LOOKBACK_DAYS = 1;
    public static final int MAX_LOOKBACK_DAYS = 365;
    public static final int MIN_LEAD_TIME_DAYS = 1;
    public static final int MAX_LEAD_TIME_DAYS = 90;

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public ReorderResponse getRecommendations(
            Integer lookbackDays,
            Integer leadTimeDays,
            ReorderPriority priority,
            UUID categoryId,
            Boolean flaggedOnly
    ) {
        int lookback = validateLookback(lookbackDays);
        int leadTime = validateLeadTime(leadTimeDays);
        boolean onlyFlagged = flaggedOnly == null || flaggedOnly;

        LocalDateTime since = LocalDateTime.now().minusDays(lookback);

        // Fetch demand velocity in the lookback window
        Map<UUID, Long> outboundMap = stockMovementRepository.sumOutboundByProductSince(since)
                .stream()
                .collect(Collectors.toMap(
                        ProductOutboundProjection::getProductId,
                        ProductOutboundProjection::getTotalOutbound,
                        Long::sum
                ));

        // Evaluate all active products
        List<Product> products = productRepository.findAll();
        List<ReorderRecommendationDto> evaluated = new ArrayList<>();

        for (Product product : products) {
            if (Boolean.FALSE.equals(product.getActive())) {
                continue;
            }
            if (categoryId != null && (product.getCategory() == null || !categoryId.equals(product.getCategory().getId()))) {
                continue;
            }

            long totalOutbound = outboundMap.getOrDefault(product.getId(), 0L);
            ReorderRecommendationDto rec = evaluateProduct(product, totalOutbound, lookback, leadTime);
            evaluated.add(rec);
        }

        // Compute summary metrics across all evaluated products
        long totalEvaluated = evaluated.size();
        long criticalCount = evaluated.stream().filter(r -> r.getPriority() == ReorderPriority.CRITICAL).count();
        long highCount = evaluated.stream().filter(r -> r.getPriority() == ReorderPriority.HIGH).count();
        long mediumCount = evaluated.stream().filter(r -> r.getPriority() == ReorderPriority.MEDIUM).count();
        long totalFlagged = criticalCount + highCount + mediumCount;

        BigDecimal totalCost = evaluated.stream()
                .filter(r -> r.getRuleApplied() != ReorderRule.HEALTHY)
                .map(ReorderRecommendationDto::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReorderSummaryDto summary = ReorderSummaryDto.builder()
                .totalEvaluated(totalEvaluated)
                .totalFlagged(totalFlagged)
                .criticalCount(criticalCount)
                .highCount(highCount)
                .mediumCount(mediumCount)
                .totalEstimatedRestockCost(totalCost)
                .build();

        // Apply filters & sorting
        List<ReorderRecommendationDto> filtered = evaluated.stream()
                .filter(r -> !onlyFlagged || r.getRuleApplied() != ReorderRule.HEALTHY)
                .filter(r -> priority == null || r.getPriority() == priority)
                .sorted(Comparator
                        .comparingInt((ReorderRecommendationDto r) -> r.getPriority().ordinal())
                        .thenComparing(r -> r.getDaysOfCover() != null ? r.getDaysOfCover() : Double.MAX_VALUE)
                        .thenComparing(ReorderRecommendationDto::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return ReorderResponse.builder()
                .lookbackDays(lookback)
                .leadTimeDays(leadTime)
                .summary(summary)
                .recommendations(filtered)
                .build();
    }

    public static ReorderRecommendationDto evaluateProduct(
            Product product,
            long totalOutbound,
            int lookbackDays,
            int leadTimeDays
    ) {
        int currentStock = product.getQuantity() != null ? product.getQuantity() : 0;
        int safetyStock = product.getMinStock() != null ? product.getMinStock() : 0;
        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;

        // 1. Average Daily Demand
        double avgDailyDemand = BigDecimal.valueOf(totalOutbound)
                .divide(BigDecimal.valueOf(lookbackDays), 2, RoundingMode.HALF_UP)
                .doubleValue();

        // 2. Days of Cover
        Double daysOfCover = null;
        if (avgDailyDemand > 0.0) {
            daysOfCover = BigDecimal.valueOf(currentStock)
                    .divide(BigDecimal.valueOf(avgDailyDemand), 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 3. Reorder Point (ROP) = (Demand * LeadTime) + SafetyStock
        int leadTimeDemand = (int) Math.ceil(avgDailyDemand * leadTimeDays);
        int reorderPoint = leadTimeDemand + safetyStock;

        // 4. Rule Evaluation & Priority Assignment
        ReorderRule ruleApplied;
        ReorderPriority priority;
        String reason;

        if (currentStock <= safetyStock) {
            ruleApplied = ReorderRule.CRITICAL_DEFICIT;
            priority = ReorderPriority.CRITICAL;
            if (currentStock <= 0) {
                reason = String.format("Stockout: Item has 0 units on hand (minimum safety stock is %d).", safetyStock);
            } else {
                reason = String.format("Critical deficit: Stock (%d) is at or below safety stock threshold (%d).", currentStock, safetyStock);
            }
        } else if (daysOfCover != null && daysOfCover <= leadTimeDays) {
            ruleApplied = ReorderRule.VELOCITY_RUNOUT;
            priority = ReorderPriority.HIGH;
            reason = String.format("Velocity risk: At %.2f units/day, current inventory will run out in %.1f days (within %d-day supplier lead time).",
                    avgDailyDemand, daysOfCover, leadTimeDays);
        } else if (currentStock <= reorderPoint) {
            ruleApplied = ReorderRule.ROP_BREACH;
            priority = ReorderPriority.MEDIUM;
            reason = String.format("Reorder point reached: Inventory (%d) has dropped below computed ROP threshold of %d units.",
                    currentStock, reorderPoint);
        } else {
            ruleApplied = ReorderRule.HEALTHY;
            priority = ReorderPriority.LOW;
            if (daysOfCover != null) {
                reason = String.format("Stock level is healthy: Estimated cover is %.1f days, well above ROP (%d).", daysOfCover, reorderPoint);
            } else {
                reason = String.format("Stock level is healthy: No outbound consumption recorded in past %d days.", lookbackDays);
            }
        }

        // 5. Recommended Order Quantity (ROQ)
        int recommendedQuantity = 0;
        if (ruleApplied != ReorderRule.HEALTHY) {
            // Target inventory is double the ROP to build an adequate replenishment cycle
            int targetStock = Math.max(reorderPoint * 2, safetyStock * 2);
            int deficit = targetStock - currentStock;
            recommendedQuantity = Math.max(deficit, 1);
        }

        BigDecimal estimatedCost = price.multiply(BigDecimal.valueOf(recommendedQuantity)).setScale(2, RoundingMode.HALF_UP);

        // 6. Build transparent rule inputs explanation
        String ropFormula = String.format("(%.2f demand/day * %d days lead time) + %d safety stock = %d ROP",
                avgDailyDemand, leadTimeDays, safetyStock, reorderPoint);
        String docFormula = avgDailyDemand > 0.0
                ? String.format("%d current stock / %.2f demand/day = %.1f days", currentStock, avgDailyDemand, daysOfCover)
                : "No outbound demand detected (infinite days of cover)";
        String roqFormula = ruleApplied != ReorderRule.HEALTHY
                ? String.format("max(target stock (%d) - current stock (%d), 1) = %d units",
                    Math.max(reorderPoint * 2, safetyStock * 2), currentStock, recommendedQuantity)
                : "0 units (healthy stock)";

        RuleInputsDto ruleInputs = RuleInputsDto.builder()
                .currentStock(currentStock)
                .safetyStock(safetyStock)
                .avgDailyDemand(avgDailyDemand)
                .leadTimeDays(leadTimeDays)
                .reorderPoint(reorderPoint)
                .daysOfCover(daysOfCover)
                .reorderPointFormula(ropFormula)
                .daysOfCoverFormula(docFormula)
                .recommendedQuantityFormula(roqFormula)
                .build();

        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Unassigned";
        UUID categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String supplierName = product.getSupplier() != null ? product.getSupplier().getName() : "No Supplier";
        String supplierEmail = product.getSupplier() != null ? product.getSupplier().getEmail() : null;
        UUID supplierId = product.getSupplier() != null ? product.getSupplier().getId() : null;

        return ReorderRecommendationDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .supplierEmail(supplierEmail)
                .unitPrice(price)
                .currentStock(currentStock)
                .safetyStock(safetyStock)
                .leadTimeDays(leadTimeDays)
                .avgDailyDemand(avgDailyDemand)
                .daysOfCover(daysOfCover)
                .reorderPoint(reorderPoint)
                .recommendedQuantity(recommendedQuantity)
                .estimatedCost(estimatedCost)
                .priority(priority)
                .ruleApplied(ruleApplied)
                .reason(reason)
                .ruleInputs(ruleInputs)
                .build();
    }

    private int validateLookback(Integer lookbackDays) {
        if (lookbackDays == null) {
            return DEFAULT_LOOKBACK_DAYS;
        }
        if (lookbackDays < MIN_LOOKBACK_DAYS || lookbackDays > MAX_LOOKBACK_DAYS) {
            throw new IllegalArgumentException(String.format(
                    "lookbackDays must be between %d and %d (received %d)",
                    MIN_LOOKBACK_DAYS, MAX_LOOKBACK_DAYS, lookbackDays));
        }
        return lookbackDays;
    }

    private int validateLeadTime(Integer leadTimeDays) {
        if (leadTimeDays == null) {
            return DEFAULT_LEAD_TIME_DAYS;
        }
        if (leadTimeDays < MIN_LEAD_TIME_DAYS || leadTimeDays > MAX_LEAD_TIME_DAYS) {
            throw new IllegalArgumentException(String.format(
                    "leadTimeDays must be between %d and %d (received %d)",
                    MIN_LEAD_TIME_DAYS, MAX_LEAD_TIME_DAYS, leadTimeDays));
        }
        return leadTimeDays;
    }
}
