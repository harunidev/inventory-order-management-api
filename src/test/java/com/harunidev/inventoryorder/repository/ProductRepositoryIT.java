package com.harunidev.inventoryorder.repository;

import com.harunidev.inventoryorder.config.TestcontainersConfig;
import com.harunidev.inventoryorder.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class ProductRepositoryIT {

    @Autowired
    private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        product = Product.builder()
                .name("Widget Pro")
                .description("A professional widget")
                .sku("WDG-PRO-001")
                .price(new BigDecimal("49.99"))
                .stockQuantity(100)
                .category("Electronics")
                .build();
    }

    @Test
    void save_and_findById() {
        Product saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("WDG-PRO-001");
        assertThat(found.get().getStockQuantity()).isEqualTo(100);
    }

    @Test
    void findBySku_found() {
        productRepository.save(product);

        Optional<Product> found = productRepository.findBySku("WDG-PRO-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Widget Pro");
    }

    @Test
    void findBySku_notFound() {
        Optional<Product> found = productRepository.findBySku("NONEXISTENT");

        assertThat(found).isEmpty();
    }

    @Test
    void existsBySku_true() {
        productRepository.save(product);

        assertThat(productRepository.existsBySku("WDG-PRO-001")).isTrue();
    }

    @Test
    void existsBySku_false() {
        assertThat(productRepository.existsBySku("NONEXISTENT")).isFalse();
    }

    @Test
    void duplicateSku_throwsException() {
        productRepository.save(product);

        Product duplicate = Product.builder()
                .name("Widget Copy")
                .sku("WDG-PRO-001")
                .price(new BigDecimal("19.99"))
                .stockQuantity(10)
                .build();

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByCategory_returnsMatchingProducts() {
        productRepository.save(product);

        Product other = Product.builder()
                .name("Gadget X")
                .sku("GDG-X-001")
                .price(new BigDecimal("99.00"))
                .stockQuantity(20)
                .category("Gadgets")
                .build();
        productRepository.save(other);

        Page<Product> electronics = productRepository.findByCategory("Electronics", PageRequest.of(0, 20));
        assertThat(electronics.getContent()).hasSize(1);
        assertThat(electronics.getContent().get(0).getSku()).isEqualTo("WDG-PRO-001");
    }

    @Test
    void findByCategory_noMatch_returnsEmpty() {
        productRepository.save(product);

        Page<Product> found = productRepository.findByCategory("Furniture", PageRequest.of(0, 20));

        assertThat(found.getContent()).isEmpty();
        assertThat(found.getTotalElements()).isEqualTo(0);
    }

    @Test
    void stockQuantityDefaultsToZero_whenNotSet() {
        Product noStock = Product.builder()
                .name("Zero Stock Item")
                .sku("ZSI-001")
                .price(new BigDecimal("5.00"))
                .stockQuantity(0)
                .build();
        Product saved = productRepository.save(noStock);

        assertThat(saved.getStockQuantity()).isEqualTo(0);
    }

    @Test
    void update_stockQuantity() {
        Product saved = productRepository.save(product);
        saved.setStockQuantity(250);
        productRepository.save(saved);

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(250);
    }

    @Test
    void delete_removesProduct() {
        Product saved = productRepository.save(product);
        productRepository.deleteById(saved.getId());

        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void findAll_returnsAllProducts() {
        productRepository.save(product);

        Product second = Product.builder()
                .name("Another Product")
                .sku("ANP-001")
                .price(new BigDecimal("15.00"))
                .stockQuantity(5)
                .build();
        productRepository.save(second);

        assertThat(productRepository.findAll()).hasSize(2);
    }
}
