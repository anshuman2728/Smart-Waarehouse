package com.saadaoui.smartwarehouse.reorder.service;

import com.saadaoui.smartwarehouse.reorder.dto.ReorderPriority;
import com.saadaoui.smartwarehouse.reorder.dto.ReorderResponse;

import java.util.UUID;

public interface ReorderService {

    ReorderResponse getRecommendations(
            Integer lookbackDays,
            Integer leadTimeDays,
            ReorderPriority priority,
            UUID categoryId,
            Boolean flaggedOnly
    );

}
