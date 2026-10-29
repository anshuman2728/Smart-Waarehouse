package com.saadaoui.smartwarehouse.reorder.controller;

import com.saadaoui.smartwarehouse.reorder.dto.*;
import com.saadaoui.smartwarehouse.reorder.service.ReorderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReorderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReorderService reorderService;

    @InjectMocks
    private ReorderController reorderController;

    private ReorderResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reorderController).build();

        ReorderRecommendationDto rec = ReorderRecommendationDto.builder()
                .productId(UUID.randomUUID())
                .productName("Industrial Sensor X")
                .sku("SN-902")
                .categoryName("Electronics")
                .supplierName("Apex Ltd")
                .currentStock(5)
                .safetyStock(10)
                .leadTimeDays(7)
                .avgDailyDemand(2.0)
                .daysOfCover(2.5)
                .reorderPoint(24)
                .recommendedQuantity(43)
                .estimatedCost(new BigDecimal("1075.00"))
                .priority(ReorderPriority.CRITICAL)
                .ruleApplied(ReorderRule.CRITICAL_DEFICIT)
                .reason("Critical deficit: Stock (5) is at or below safety stock threshold (10).")
                .ruleInputs(RuleInputsDto.builder()
                        .currentStock(5)
                        .safetyStock(10)
                        .avgDailyDemand(2.0)
                        .leadTimeDays(7)
                        .reorderPoint(24)
                        .daysOfCover(2.5)
                        .reorderPointFormula("(2.00 * 7) + 10 = 24 ROP")
                        .daysOfCoverFormula("5 / 2.00 = 2.5 days")
                        .build())
                .build();

        ReorderSummaryDto summary = ReorderSummaryDto.builder()
                .totalEvaluated(1)
                .totalFlagged(1)
                .criticalCount(1)
                .highCount(0)
                .mediumCount(0)
                .totalEstimatedRestockCost(new BigDecimal("1075.00"))
                .build();

        mockResponse = ReorderResponse.builder()
                .lookbackDays(30)
                .leadTimeDays(7)
                .summary(summary)
                .recommendations(List.of(rec))
                .build();
    }

    @Test
    @DisplayName("GET /api/reorder/recommendations returns 200 and JSON with recommendation details")
    void testGetRecommendations() throws Exception {
        when(reorderService.getRecommendations(any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/reorder/recommendations")
                        .param("lookbackDays", "30")
                        .param("leadTimeDays", "7")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lookbackDays").value(30))
                .andExpect(jsonPath("$.leadTimeDays").value(7))
                .andExpect(jsonPath("$.summary.totalFlagged").value(1))
                .andExpect(jsonPath("$.recommendations[0].sku").value("SN-902"))
                .andExpect(jsonPath("$.recommendations[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$.recommendations[0].ruleApplied").value("CRITICAL_DEFICIT"))
                .andExpect(jsonPath("$.recommendations[0].ruleInputs.reorderPoint").value(24));
    }

    @Test
    @DisplayName("GET /api/reorder/export returns CSV with headers and formatted rows")
    void testExportRecommendationsCsv() throws Exception {
        when(reorderService.getRecommendations(any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/reorder/export")
                        .param("lookbackDays", "30")
                        .param("leadTimeDays", "7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"reorder-recommendations.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Product,SKU,Category,Supplier,Priority")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Industrial Sensor X,SN-902,Electronics,Apex Ltd,CRITICAL")));
    }
}
