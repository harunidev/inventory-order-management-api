package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.config.TestcontainersConfig;
import com.harunidev.inventoryorder.dto.request.OrderItemRequest;
import com.harunidev.inventoryorder.dto.request.OrderRequest;
import com.harunidev.inventoryorder.dto.response.OrderResponse;
import com.harunidev.inventoryorder.entity.*;
import com.harunidev.inventoryorder.exception.InsufficientStockException;
import com.harunidev.inventoryorder.repository.OrderRepository;
import com.harunidev.inventoryorder.repository.ProductRepository;
import com.harunidev.inventoryorder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class OrderServiceIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .username("testuser")
                .email("testuser@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .build());

        product = productRepository.save(Product.builder()
                .name("Integration Widget")
                .sku("IT-WDG-001")
                .price(new BigDecimal("30.00"))
                .stockQuantity(10)
                .category("Test")
                .build());
    }

    private OrderRequest buildRequest(int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(quantity);

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(item));
        req.setNotes("Integration test order");
        return req;
    }

    @Test
    void createOrder_deductsStockAndPersists() {
        OrderResponse response = orderService.createOrder(buildRequest(3), "testuser");

        assertThat(response.getId()).isNotNull();
        assertThat(response.getOrderNumber()).startsWith("ORD-");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductSku()).isEqualTo("IT-WDG-001");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);

        // Verify stock was actually deducted in the database
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void createOrder_insufficientStock_doesNotSaveOrder() {
        assertThatThrownBy(() -> orderService.createOrder(buildRequest(11), "testuser"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Integration Widget")
                .hasMessageContaining("Available: 10");

        // Verify no order was persisted
        assertThat(orderRepository.findAll()).isEmpty();

        // Verify stock was not touched
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void cancelOrder_restoresStock() {
        OrderResponse created = orderService.createOrder(buildRequest(4), "testuser");
        Long orderId = created.getId();

        // Verify stock deducted
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(6);

        orderService.cancelOrder(orderId, "testuser");

        // Verify stock restored
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(10);

        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void createOrder_exactStock_succeeds() {
        // Order exactly as many as available
        OrderResponse response = orderService.createOrder(buildRequest(10), "testuser");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(0);
    }

    @Test
    void updateOrderStatus_toShipped_persistsStatus() {
        OrderResponse created = orderService.createOrder(buildRequest(1), "testuser");

        orderService.updateOrderStatus(created.getId(), OrderStatus.SHIPPED);

        Order order = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void getMyOrders_returnsOnlyUserOrders() {
        orderService.createOrder(buildRequest(1), "testuser");
        orderService.createOrder(buildRequest(2), "testuser");

        List<OrderResponse> myOrders = orderService.getMyOrders("testuser");

        assertThat(myOrders).hasSize(2);
        assertThat(myOrders).allMatch(o -> o.getUsername().equals("testuser"));
    }
}
