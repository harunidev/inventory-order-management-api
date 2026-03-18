package com.harunidev.inventoryorder.service;

import com.harunidev.inventoryorder.dto.request.OrderItemRequest;
import com.harunidev.inventoryorder.dto.request.OrderRequest;
import com.harunidev.inventoryorder.dto.response.OrderResponse;
import com.harunidev.inventoryorder.entity.*;
import com.harunidev.inventoryorder.exception.InsufficientStockException;
import com.harunidev.inventoryorder.exception.ResourceNotFoundException;
import com.harunidev.inventoryorder.exception.UnauthorizedAccessException;
import com.harunidev.inventoryorder.repository.OrderRepository;
import com.harunidev.inventoryorder.repository.ProductRepository;
import com.harunidev.inventoryorder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User regularUser;
    private User adminUser;
    private Product product;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@test.com")
                .password("encoded")
                .role(Role.ADMIN)
                .build();

        product = Product.builder()
                .id(10L)
                .name("Widget")
                .sku("WDG-001")
                .price(new BigDecimal("25.00"))
                .stockQuantity(50)
                .build();
    }

    private OrderRequest buildOrderRequest(Long productId, int quantity) {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(quantity);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemReq));
        request.setNotes("Test order");
        return request;
    }

    private Order buildSavedOrder(User user, Product prod, int qty) {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(prod)
                .quantity(qty)
                .unitPrice(prod.getPrice())
                .subtotal(prod.getPrice().multiply(BigDecimal.valueOf(qty)))
                .build();

        Order order = Order.builder()
                .id(100L)
                .orderNumber("ORD-TESTABCD")
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(prod.getPrice().multiply(BigDecimal.valueOf(qty)))
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);
        return order;
    }

    // ─── createOrder ─────────────────────────────────────────────────────────

    @Test
    void createOrder_success() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        // Phase 3: createOrder uses findByIdWithLock for pessimistic locking
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Order savedOrder = buildSavedOrder(regularUser, product, 3);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(buildOrderRequest(10L, 3), "user1");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-TESTABCD");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(productRepository, atLeastOnce()).save(any(Product.class));
    }

    @Test
    void createOrder_insufficientStock_throwsException() {
        product.setStockQuantity(2);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(buildOrderRequest(10L, 5), "user1"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Widget")
                .hasMessageContaining("Requested: 5")
                .hasMessageContaining("Available: 2");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_productNotFound_throwsException() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(buildOrderRequest(99L, 1), "user1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_zeroStock_throwsException() {
        product.setStockQuantity(0);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(productRepository.findByIdWithLock(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(buildOrderRequest(10L, 1), "user1"))
                .isInstanceOf(InsufficientStockException.class);
    }

    // ─── getOrderById ─────────────────────────────────────────────────────────

    @Test
    void getOrderById_ownerCanView() {
        Order order = buildSavedOrder(regularUser, product, 2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        OrderResponse response = orderService.getOrderById(100L, "user1");

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getOrderById_adminCanViewOthersOrder() {
        Order order = buildSavedOrder(regularUser, product, 2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        OrderResponse response = orderService.getOrderById(100L, "admin");

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getOrderById_otherUserCannotView_throwsException() {
        Order order = buildSavedOrder(regularUser, product, 2);

        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .email("other@test.com")
                .role(Role.USER)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> orderService.getOrderById(100L, "other"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // ─── cancelOrder ─────────────────────────────────────────────────────────

    @Test
    void cancelOrder_ownerCanCancelPending() {
        Order order = buildSavedOrder(regularUser, product, 2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        orderService.cancelOrder(100L, "user1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void cancelOrder_adminCanCancelAnyOrder() {
        Order order = buildSavedOrder(regularUser, product, 2);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        orderService.cancelOrder(100L, "admin");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_otherUserCannotCancel_throwsException() {
        Order order = buildSavedOrder(regularUser, product, 2);

        User otherUser = User.builder()
                .id(99L)
                .username("other")
                .role(Role.USER)
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, "other"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void cancelOrder_shippedOrder_throwsException() {
        Order order = buildSavedOrder(regularUser, product, 2);
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> orderService.cancelOrder(100L, "user1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHIPPED");
    }

    // ─── updateOrderStatus ────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_toCancelled_restoresStock() {
        Order order = buildSavedOrder(regularUser, product, 3);
        int originalStock = product.getStockQuantity();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(100L, OrderStatus.CANCELLED);

        assertThat(product.getStockQuantity()).isEqualTo(originalStock + 3);
    }

    @Test
    void updateOrderStatus_toConfirmed_doesNotRestoreStock() {
        Order order = buildSavedOrder(regularUser, product, 3);
        int originalStock = product.getStockQuantity();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(100L, OrderStatus.CONFIRMED);

        assertThat(product.getStockQuantity()).isEqualTo(originalStock);
        verify(productRepository, never()).save(any());
    }

    // ─── getMyOrders ─────────────────────────────────────────────────────────

    @Test
    void getMyOrders_returnsUserOrders() {
        Order order = buildSavedOrder(regularUser, product, 1);

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(regularUser));
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getMyOrders("user1");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUsername()).isEqualTo("user1");
    }
}
