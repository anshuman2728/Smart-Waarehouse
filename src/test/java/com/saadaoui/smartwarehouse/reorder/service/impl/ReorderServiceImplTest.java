package com.saadaoui.smartwarehouse.reorder.service.impl;

import com.saadaoui.smartwarehouse.entity.Category;
import com.saadaoui.smartwarehouse.entity.Product;
import com.saadaoui.smartwarehouse.entity.Supplier;
import com.saadaoui.smartwarehouse.movement.repository.StockMovementRepository;
import com.saadaoui.smartwarehouse.movement.repository.StockMovementRepository.ProductOutboundProjection;
import com.saadaoui.smartwarehouse.product.repository.ProductRepository;
import com.saadaoui.smartwarehouse.reorder.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private ReorderServiceImpl reorderService;

    private Category category;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Industrial Components")
                .build();

        supplier = Supplier.builder()
                .id(UUID.randomUUID())
                .name("Acme Supplies")
                .email("orders@acme.com")
                .build();
    }

    private Product buildProduct(String name, String sku, int qty, int minStock, BigDecimal price) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .sku(sku)
                .quantity(qty)
                .minStock(minStock)
                .price(price)
                .active(true)
                .category(category)
                .supplier(supplier)
                .build();
    }

    @Test
    @DisplayName("Boundary Case: Exact safety stock triggers CRITICAL_DEFICIT")
    void testExactSafetyStockTriggersCritical() {
        Product product = buildProduct("Bearing A", "BR-01", 10, 10, new BigDecimal("25.00"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 30L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.CRITICAL);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.CRITICAL_DEFICIT);
        assertThat(rec.getRecommendedQuantity()).isGreaterThan(0);
        assertThat(rec.getReason()).contains("Critical deficit");
    }

    @Test
    @DisplayName("Boundary Case: Zero stockout triggers CRITICAL_DEFICIT with stockout explanation")
    void testZeroStockTriggersStockout() {
        Product product = buildProduct("Bolt M8", "BLT-08", 0, 15, new BigDecimal("1.50"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 60L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.CRITICAL);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.CRITICAL_DEFICIT);
        assertThat(rec.getRecommendedQuantity()).isGreaterThan(0);
        assertThat(rec.getReason()).contains("Stockout: Item has 0 units on hand");
    }

    @Test
    @DisplayName("Boundary Case: Stock above safety stock but depleting within lead time triggers VELOCITY_RUNOUT")
    void testDepletionWithinLeadTimeTriggersVelocityRunout() {
        // Current: 20 units, MinStock: 5.
        // Outbound in 30 days = 120 units -> 4 units/day.
        // Days of cover = 20 / 4 = 5.0 days.
        // Lead time = 7 days. 5.0 <= 7 -> will run out before supplier arrives!
        Product product = buildProduct("Hydraulic Seal", "HS-22", 20, 5, new BigDecimal("12.00"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 120L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.HIGH);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.VELOCITY_RUNOUT);
        assertThat(rec.getAvgDailyDemand()).isEqualTo(4.0);
        assertThat(rec.getDaysOfCover()).isEqualTo(5.0);
        assertThat(rec.getReason()).contains("Velocity risk");
        assertThat(rec.getRuleInputs().getDaysOfCoverFormula()).contains("5.0 days");
    }

    @Test
    @DisplayName("Boundary Case: Exact lead time boundary triggers VELOCITY_RUNOUT")
    void testExactLeadTimeBoundary() {
        // Current: 14 units, MinStock: 2. Outbound: 60 in 30 days -> 2 units/day.
        // Days of cover = 14 / 2 = 7.0 days. Lead time = 7 days.
        Product product = buildProduct("Filter Element", "FE-10", 14, 2, new BigDecimal("45.00"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 60L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.HIGH);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.VELOCITY_RUNOUT);
        assertThat(rec.getDaysOfCover()).isEqualTo(7.0);
    }

    @Test
    @DisplayName("Boundary Case: Below Reorder Point (ROP) with safe lead time triggers ROP_BREACH")
    void testRopBreachTriggersMedium() {
        // Current: 25 units, MinStock: 10.
        // Outbound: 60 in 30 days -> 2 units/day.
        // Lead time: 7 days. Lead time demand = 14.
        // ROP = 14 + 10 = 24.
        // Days of cover = 25 / 2 = 12.5 days (> 7 days).
        // Let's set Current to 22 (<= 24 ROP, but > 10 safety stock):
        Product product = buildProduct("Motor Brush", "MB-50", 22, 10, new BigDecimal("8.00"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 60L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.MEDIUM);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.ROP_BREACH);
        assertThat(rec.getReorderPoint()).isEqualTo(24);
        assertThat(rec.getReason()).contains("Reorder point reached");
    }

    @Test
    @DisplayName("Boundary Case: Healthy inventory above ROP triggers HEALTHY")
    void testHealthyStock() {
        // Current: 100 units, MinStock: 10, Outbound: 30 in 30 days -> 1 unit/day.
        // ROP = (1 * 7) + 10 = 17.
        // Current 100 > 17 ROP.
        Product product = buildProduct("Steel Rivet", "SR-100", 100, 10, new BigDecimal("0.50"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 30L, 30, 7);

        assertThat(rec.getPriority()).isEqualTo(ReorderPriority.LOW);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.HEALTHY);
        assertThat(rec.getRecommendedQuantity()).isEqualTo(0);
        assertThat(rec.getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Boundary Case: Zero demand in lookback period handles division by zero safely")
    void testZeroDemandHandlesDivisionSafely() {
        Product product = buildProduct("Obsolete Gear", "OG-00", 50, 5, new BigDecimal("100.00"));
        ReorderRecommendationDto rec = ReorderServiceImpl.evaluateProduct(product, 0L, 30, 7);

        assertThat(rec.getAvgDailyDemand()).isEqualTo(0.0);
        assertThat(rec.getDaysOfCover()).isNull();
        assertThat(rec.getReorderPoint()).isEqualTo(5);
        assertThat(rec.getRuleApplied()).isEqualTo(ReorderRule.HEALTHY);
        assertThat(rec.getRuleInputs().getDaysOfCoverFormula()).contains("No outbound demand detected");
    }

    @Test
    @DisplayName("Validation: Invalid lookbackDays values throw IllegalArgumentException")
    void testInvalidLookbackDaysValidation() {
        assertThatThrownBy(() -> reorderService.getRecommendations(0, 7, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lookbackDays must be between");

        assertThatThrownBy(() -> reorderService.getRecommendations(400, 7, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lookbackDays must be between");
    }

    @Test
    @DisplayName("Validation: Invalid leadTimeDays values throw IllegalArgumentException")
    void testInvalidLeadTimeDaysValidation() {
        assertThatThrownBy(() -> reorderService.getRecommendations(30, 0, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leadTimeDays must be between");

        assertThatThrownBy(() -> reorderService.getRecommendations(30, 100, null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leadTimeDays must be between");
    }

    @Test
    @DisplayName("Service Flow: Correctly aggregates metrics and applies priority filtering")
    void testServiceAggregationAndFiltering() {
        Product p1 = buildProduct("Critical Item", "CR-01", 2, 10, new BigDecimal("20.00"));
        Product p2 = buildProduct("Healthy Item", "HL-01", 100, 5, new BigDecimal("10.00"));

        ProductOutboundProjection proj1 = mock(ProductOutboundProjection.class);
        when(proj1.getProductId()).thenReturn(p1.getId());
        when(proj1.getTotalOutbound()).thenReturn(30L);

        when(stockMovementRepository.sumOutboundByProductSince(any(LocalDateTime.class)))
                .thenReturn(List.of(proj1));
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        // When requesting only flagged items
        ReorderResponse response = reorderService.getRecommendations(30, 7, null, null, true);

        assertThat(response.getSummary().getTotalEvaluated()).isEqualTo(2);
        assertThat(response.getSummary().getTotalFlagged()).isEqualTo(1);
        assertThat(response.getSummary().getCriticalCount()).isEqualTo(1);
        assertThat(response.getRecommendations()).hasSize(1);
        assertThat(response.getRecommendations().get(0).getProductId()).isEqualTo(p1.getId());
    }
}
