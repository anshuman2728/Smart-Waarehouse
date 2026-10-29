package com.saadaoui.smartwarehouse.reorder.controller;

import com.saadaoui.smartwarehouse.reorder.dto.ReorderPriority;
import com.saadaoui.smartwarehouse.reorder.dto.ReorderRecommendationDto;
import com.saadaoui.smartwarehouse.reorder.dto.ReorderResponse;
import com.saadaoui.smartwarehouse.reorder.service.ReorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/reorder")
@RequiredArgsConstructor
public class ReorderController {

    private final ReorderService reorderService;

    @GetMapping(value = "/recommendations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReorderResponse> getRecommendations(
            @RequestParam(required = false) Integer lookbackDays,
            @RequestParam(required = false) Integer leadTimeDays,
            @RequestParam(required = false) ReorderPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "true") Boolean flaggedOnly
    ) {
        ReorderResponse response = reorderService.getRecommendations(
                lookbackDays, leadTimeDays, priority, categoryId, flaggedOnly);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportRecommendations(
            @RequestParam(required = false) Integer lookbackDays,
            @RequestParam(required = false) Integer leadTimeDays,
            @RequestParam(required = false) ReorderPriority priority,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "true") Boolean flaggedOnly
    ) {
        ReorderResponse response = reorderService.getRecommendations(
                lookbackDays, leadTimeDays, priority, categoryId, flaggedOnly);

        StringBuilder sb = new StringBuilder();
        sb.append("Product,SKU,Category,Supplier,Priority,Current Stock,Safety Stock,Daily Demand,Days of Cover,Reorder Point,Recommended Order,Est. Cost,Rule Applied,Reason\n");

        for (ReorderRecommendationDto r : response.getRecommendations()) {
            sb.append(escapeCsv(r.getProductName())).append(",")
              .append(escapeCsv(r.getSku())).append(",")
              .append(escapeCsv(r.getCategoryName())).append(",")
              .append(escapeCsv(r.getSupplierName())).append(",")
              .append(r.getPriority()).append(",")
              .append(r.getCurrentStock()).append(",")
              .append(r.getSafetyStock()).append(",")
              .append(r.getAvgDailyDemand()).append(",")
              .append(r.getDaysOfCover() != null ? r.getDaysOfCover() : "N/A").append(",")
              .append(r.getReorderPoint()).append(",")
              .append(r.getRecommendedQuantity()).append(",")
              .append(r.getEstimatedCost()).append(",")
              .append(r.getRuleApplied()).append(",")
              .append(escapeCsv(r.getReason())).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("reorder-recommendations.csv").build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
