package com.harunidev.inventoryorder.repository;

import com.harunidev.inventoryorder.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    List<Product> findByCategory(String category);
    // TODO Phase 2: Page<Product> findAll(Pageable pageable);
    // TODO Phase 2: List<Product> findByStockQuantityLessThan(int threshold);
}
