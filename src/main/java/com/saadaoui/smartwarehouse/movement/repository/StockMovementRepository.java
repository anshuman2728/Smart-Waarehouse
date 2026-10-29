package com.saadaoui.smartwarehouse.movement.repository;

import com.saadaoui.smartwarehouse.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    List<StockMovement> findTop5ByOrderByCreatedAtDesc();

    List<StockMovement> findByCreatedAtAfter(java.time.LocalDateTime date);

    List<StockMovement> findByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT m.product.name AS name, COUNT(m.id) AS movementCount
            FROM StockMovement m
            WHERE (:from IS NULL OR m.createdAt >= :from)
              AND (:to IS NULL OR m.createdAt <= :to)
            GROUP BY m.product.id, m.product.name
            ORDER BY COUNT(m.id) DESC
            """)
    List<TopProductProjection> findTopProductsByMovement(
            java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable);

    interface TopProductProjection {
        String getName();
        long getMovementCount();
    }

    @Query("""
            SELECT m.product.id AS productId, COALESCE(SUM(m.quantity), 0) AS totalOutbound
            FROM StockMovement m
            WHERE m.type = 'OUTBOUND' AND m.createdAt >= :since
            GROUP BY m.product.id
            """)
    List<ProductOutboundProjection> sumOutboundByProductSince(java.time.LocalDateTime since);

    interface ProductOutboundProjection {
        UUID getProductId();
        long getTotalOutbound();
    }

}
