package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.ProductRequest;
import com.harunidev.inventoryorder.dto.response.ProductResponse;
import com.harunidev.inventoryorder.entity.Product;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("Product with SKU '" + request.getSku() + "' already exists");
        }
        Product product = mapToEntity(request);
        return mapToResponse(productRepository.save(product));
    }

    public ProductResponse getProductById(Long id) {
        return mapToResponse(findById(id));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findById(id);

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("Product with SKU '" + request.getSku() + "' already exists");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        if (quantity < 0) {
            throw new IllegalStateException("Stock quantity cannot be negative");
        }
        Product product = findById(id);
        product.setStockQuantity(quantity);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        findById(id);
        productRepository.deleteById(id);
    }

    Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private Product mapToEntity(ProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .build();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
