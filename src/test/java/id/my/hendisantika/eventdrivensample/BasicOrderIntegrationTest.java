package id.my.hendisantika.eventdrivensample;

import id.my.hendisantika.eventdrivensample.dto.OrderRequest;
import id.my.hendisantika.eventdrivensample.dto.OrderResponse;
import id.my.hendisantika.eventdrivensample.model.Order;
import id.my.hendisantika.eventdrivensample.model.OrderStatus;
import id.my.hendisantika.eventdrivensample.repository.OrderRepository;
import id.my.hendisantika.eventdrivensample.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Basic integration test for order service and repository integration.
 * Uses PostgreSQL Testcontainer for realistic database testing.
 */
@SpringBootTest
@Testcontainers
class BasicOrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("logging.level.id.my.hendisantika.eventdrivensample", () -> "INFO");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerEmail("john@example.com");
        request.setProductName("Laptop");
        request.setQuantity(2);
        request.setUnitPrice(new BigDecimal("999.99"));

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getCustomerEmail()).isEqualTo("john@example.com");
        assertThat(response.getProductName()).isEqualTo("Laptop");
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getUnitPrice()).isEqualTo(new BigDecimal("999.99"));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getOrderNumber()).isNotNull();

        // Verify database persistence
        Optional<Order> savedOrder = orderRepository.findByOrderNumber(response.getOrderNumber());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getCustomerName()).isEqualTo("John Doe");
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void shouldCompleteOrderLifecycle() {
        // Given - Create order
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Jane Smith");
        request.setCustomerEmail("jane@example.com");
        request.setProductName("Smartphone");
        request.setQuantity(1);
        request.setUnitPrice(new BigDecimal("599.99"));

        OrderResponse createResponse = orderService.createOrder(request);
        String orderNumber = createResponse.getOrderNumber();

        // When & Then - Test each lifecycle step
        OrderResponse confirmedOrder = orderService.confirmOrder(orderNumber);
        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        OrderResponse shippedOrder = orderService.shipOrder(orderNumber);
        assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        OrderResponse deliveredOrder = orderService.deliverOrder(orderNumber);
        assertThat(deliveredOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);

        // Verify final state in database
        Optional<Order> finalOrder = orderRepository.findByOrderNumber(orderNumber);
        assertThat(finalOrder).isPresent();
        assertThat(finalOrder.get().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldCancelOrder() {
        // Given - Create order
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Bob Wilson");
        request.setCustomerEmail("bob@example.com");
        request.setProductName("Tablet");
        request.setQuantity(1);
        request.setUnitPrice(new BigDecimal("299.99"));

        OrderResponse createResponse = orderService.createOrder(request);
        String orderNumber = createResponse.getOrderNumber();

        // When - Cancel order
        OrderResponse cancelResponse = orderService.cancelOrder(orderNumber);

        // Then
        assertThat(cancelResponse.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Verify database
        Optional<Order> cancelledOrder = orderRepository.findByOrderNumber(orderNumber);
        assertThat(cancelledOrder).isPresent();
        assertThat(cancelledOrder.get().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldGetOrdersByCustomerEmail() {
        // Given - Create multiple orders for same customer
        String customerEmail = "customer@example.com";

        OrderRequest request1 = new OrderRequest();
        request1.setCustomerName("Customer");
        request1.setCustomerEmail(customerEmail);
        request1.setProductName("Product 1");
        request1.setQuantity(1);
        request1.setUnitPrice(new BigDecimal("100.00"));
        orderService.createOrder(request1);

        OrderRequest request2 = new OrderRequest();
        request2.setCustomerName("Customer");
        request2.setCustomerEmail(customerEmail);
        request2.setProductName("Product 2");
        request2.setQuantity(1);
        request2.setUnitPrice(new BigDecimal("200.00"));
        orderService.createOrder(request2);

        // When
        List<OrderResponse> orders = orderService.getOrdersByCustomerEmail(customerEmail);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(orders.get(1).getCustomerEmail()).isEqualTo(customerEmail);
    }

    @Test
    void shouldGetAllOrders() {
        // Given - Create multiple orders
        OrderRequest request1 = new OrderRequest();
        request1.setCustomerName("Customer 1");
        request1.setCustomerEmail("customer1@example.com");
        request1.setProductName("Product 1");
        request1.setQuantity(1);
        request1.setUnitPrice(new BigDecimal("100.00"));
        orderService.createOrder(request1);

        OrderRequest request2 = new OrderRequest();
        request2.setCustomerName("Customer 2");
        request2.setCustomerEmail("customer2@example.com");
        request2.setProductName("Product 2");
        request2.setQuantity(1);
        request2.setUnitPrice(new BigDecimal("200.00"));
        orderService.createOrder(request2);

        // When
        List<OrderResponse> orders = orderService.getAllOrders();

        // Then
        assertThat(orders).hasSize(2);
    }

    @Test
    void shouldThrowExceptionForNonExistentOrder() {
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderByNumber("NON-EXISTENT");
        });
    }

    @Test
    void shouldNotShipUnconfirmedOrder() {
        // Given - Create order but don't confirm
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Test User");
        request.setCustomerEmail("test@example.com");
        request.setProductName("Product");
        request.setQuantity(1);
        request.setUnitPrice(new BigDecimal("100.00"));

        OrderResponse createResponse = orderService.createOrder(request);
        String orderNumber = createResponse.getOrderNumber();

        // When & Then - Try to ship without confirming
        assertThrows(RuntimeException.class, () -> {
            orderService.shipOrder(orderNumber);
        });
    }

    @Test
    void shouldNotCancelDeliveredOrder() {
        // Given - Create and complete order lifecycle
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Test User");
        request.setCustomerEmail("test@example.com");
        request.setProductName("Product");
        request.setQuantity(1);
        request.setUnitPrice(new BigDecimal("100.00"));

        OrderResponse createResponse = orderService.createOrder(request);
        String orderNumber = createResponse.getOrderNumber();

        // Complete lifecycle
        orderService.confirmOrder(orderNumber);
        orderService.shipOrder(orderNumber);
        orderService.deliverOrder(orderNumber);

        // When & Then - Try to cancel delivered order
        assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(orderNumber);
        });
    }

    @Test
    void shouldGetSpecificOrderByNumber() {
        // Given - Create order
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Specific User");
        request.setCustomerEmail("specific@example.com");
        request.setProductName("Specific Product");
        request.setQuantity(1);
        request.setUnitPrice(new BigDecimal("123.45"));

        OrderResponse createResponse = orderService.createOrder(request);
        String orderNumber = createResponse.getOrderNumber();

        // When
        OrderResponse response = orderService.getOrderByNumber(orderNumber);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(response.getCustomerName()).isEqualTo("Specific User");
        assertThat(response.getProductName()).isEqualTo("Specific Product");
    }
}