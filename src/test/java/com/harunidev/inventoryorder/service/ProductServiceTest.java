package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.ProductRequest;
import com.harunidev.inventoryorder.dto.response.ProductResponse;
import com.harunidev.inventoryorder.entity.Product;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .sku("SKU-001")
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .category("Electronics")
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Test Product");
        productRequest.setDescription("Test Description");
        productRequest.setSku("SKU-001");
        productRequest.setPrice(new BigDecimal("99.99"));
        productRequest.setStockQuantity(100);
        productRequest.setCategory("Electronics");
    }

    @Test
    void createProduct_success() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response.getSku()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throwsException() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(productRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU-001");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllProducts_returnsList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getProductsByCategory_returnsList() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getProductsByCategory("Electronics");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    void updateProduct_success() {
        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Updated Product");
        updateRequest.setDescription("Updated Description");
        updateRequest.setSku("SKU-002");
        updateRequest.setPrice(new BigDecimal("149.99"));
        updateRequest.setStockQuantity(50);
        updateRequest.setCategory("Gadgets");

        Product updatedProduct = Product.builder()
                .id(1L)
                .name("Updated Product")
                .sku("SKU-002")
                .price(new BigDecimal("149.99"))
                .stockQuantity(50)
                .category("Gadgets")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("SKU-002")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response.getSku()).isEqualTo("SKU-002");
        assertThat(response.getName()).isEqualTo("Updated Product");
    }

    @Test
    void updateProduct_sameSku_noConflictCheck() {
        // Updating with the same SKU should not throw even if the SKU exists
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertThat(response.getSku()).isEqualTo("SKU-001");
        verify(productRepository, never()).existsBySku(any());
    }

    @Test
    void updateProduct_duplicateSkuOnDifferentProduct_throwsException() {
        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Updated");
        updateRequest.setSku("SKU-TAKEN");
        updateRequest.setPrice(BigDecimal.TEN);
        updateRequest.setStockQuantity(10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("SKU-TAKEN")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU-TAKEN");
    }

    @Test
    void updateStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateStock(1L, 200);

        verify(productRepository).save(any(Product.class));
        assertThat(response).isNotNull();
    }

    @Test
    void updateStock_negativeQuantity_throwsException() {
        assertThatThrownBy(() -> productService.updateStock(1L, -5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negative");

        verify(productRepository, never()).findById(any());
    }

    @Test
    void deleteProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }
}
