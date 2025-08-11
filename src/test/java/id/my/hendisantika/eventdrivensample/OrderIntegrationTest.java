package id.my.hendisantika.eventdrivensample;

import id.my.hendisantika.eventdrivensample.dto.OrderRequest;
import id.my.hendisantika.eventdrivensample.dto.OrderResponse;
import id.my.hendisantika.eventdrivensample.model.Order;
import id.my.hendisantika.eventdrivensample.model.OrderStatus;
import id.my.hendisantika.eventdrivensample.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the order management REST API and database integration.
 * Tests the full flow: REST API -> Service -> Database persistence and event publishing
 * Uses PostgreSQL and Kafka Testcontainers for realistic testing environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("logging.level.id.my.hendisantika.eventdrivensample", () -> "DEBUG");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
        OrderRequest orderRequest = createOrderRequest("John Doe", "john@example.com", "Laptop", 2, new BigDecimal("999.99"));

        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                getBaseUrl(), orderRequest, OrderResponse.class);

        // Then - Verify HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getBody().getCustomerEmail()).isEqualTo("john@example.com");
        assertThat(response.getBody().getProductName()).isEqualTo("Laptop");
        assertThat(response.getBody().getQuantity()).isEqualTo(2);
        assertThat(response.getBody().getUnitPrice()).isEqualTo(new BigDecimal("999.99"));
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getBody().getOrderNumber()).isNotNull();

        // Verify database persistence
        Optional<Order> savedOrder = orderRepository.findByOrderNumber(response.getBody().getOrderNumber());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getCustomerName()).isEqualTo("John Doe");
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.get().getTotalAmount()).isEqualTo(new BigDecimal("1999.98"));
    }

    @Test
    void shouldCompleteOrderLifecycle() {
        // Given - Create order
        OrderRequest orderRequest = createOrderRequest("Jane Smith", "jane@example.com", "Smartphone", 1, new BigDecimal("599.99"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // When & Then - Test each lifecycle step
        testOrderStatusTransition(orderNumber, "confirm", OrderStatus.CONFIRMED);
        testOrderStatusTransition(orderNumber, "ship", OrderStatus.SHIPPED);
        testOrderStatusTransition(orderNumber, "deliver", OrderStatus.DELIVERED);

        // Verify final state in database
        Optional<Order> finalOrder = orderRepository.findByOrderNumber(orderNumber);
        assertThat(finalOrder).isPresent();
        assertThat(finalOrder.get().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldCancelOrder() {
        // Given - Create order
        OrderRequest orderRequest = createOrderRequest("Bob Wilson", "bob@example.com", "Tablet", 1, new BigDecimal("299.99"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // When - Cancel order
        ResponseEntity<OrderResponse> cancelResponse = restTemplate.exchange(
                getBaseUrl() + "/" + orderNumber + "/cancel", HttpMethod.PUT, null, OrderResponse.class);

        // Then
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Verify database
        Optional<Order> cancelledOrder = orderRepository.findByOrderNumber(orderNumber);
        assertThat(cancelledOrder).isPresent();
        assertThat(cancelledOrder.get().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldGetOrdersByCustomerEmail() {
        // Given - Create multiple orders for same customer
        String customerEmail = "customer@example.com";
        createOrderForCustomer("Product 1", customerEmail);
        createOrderForCustomer("Product 2", customerEmail);

        // When
        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(
                getBaseUrl() + "/customer/" + customerEmail, OrderResponse[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()[0].getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(response.getBody()[1].getCustomerEmail()).isEqualTo(customerEmail);
    }

    @Test
    void shouldGetAllOrders() {
        // Given - Create multiple orders
        createOrderForCustomer("Product 1", "customer1@example.com");
        createOrderForCustomer("Product 2", "customer2@example.com");

        // When
        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(getBaseUrl(), OrderResponse[].class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void shouldReturnNotFoundForNonExistentOrder() {
        // When
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                getBaseUrl() + "/NON-EXISTENT", OrderResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotShipUnconfirmedOrder() {
        // Given - Create order but don't confirm
        OrderRequest orderRequest = createOrderRequest("Test User", "test@example.com", "Product", 1, new BigDecimal("100.00"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // When - Try to ship without confirming
        ResponseEntity<OrderResponse> shipResponse = restTemplate.exchange(
                getBaseUrl() + "/" + orderNumber + "/ship", HttpMethod.PUT, null, OrderResponse.class);

        // Then
        assertThat(shipResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotDeliverUnshippedOrder() {
        // Given - Create and confirm order but don't ship
        OrderRequest orderRequest = createOrderRequest("Test User", "test@example.com", "Product", 1, new BigDecimal("100.00"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // Confirm order
        restTemplate.exchange(getBaseUrl() + "/" + orderNumber + "/confirm", HttpMethod.PUT, null, OrderResponse.class);

        // When - Try to deliver without shipping
        ResponseEntity<OrderResponse> deliverResponse = restTemplate.exchange(
                getBaseUrl() + "/" + orderNumber + "/deliver", HttpMethod.PUT, null, OrderResponse.class);

        // Then
        assertThat(deliverResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotCancelDeliveredOrder() {
        // Given - Create and complete order lifecycle
        OrderRequest orderRequest = createOrderRequest("Test User", "test@example.com", "Product", 1, new BigDecimal("100.00"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // Complete lifecycle: confirm -> ship -> deliver
        testOrderStatusTransition(orderNumber, "confirm", OrderStatus.CONFIRMED);
        testOrderStatusTransition(orderNumber, "ship", OrderStatus.SHIPPED);
        testOrderStatusTransition(orderNumber, "deliver", OrderStatus.DELIVERED);

        // When - Try to cancel delivered order
        ResponseEntity<OrderResponse> cancelResponse = restTemplate.exchange(
                getBaseUrl() + "/" + orderNumber + "/cancel", HttpMethod.PUT, null, OrderResponse.class);

        // Then
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldGetSpecificOrderByNumber() {
        // Given - Create order
        OrderRequest orderRequest = createOrderRequest("Specific User", "specific@example.com", "Specific Product", 1, new BigDecimal("123.45"));
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(getBaseUrl(), orderRequest, OrderResponse.class);
        String orderNumber = createResponse.getBody().getOrderNumber();

        // When
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
                getBaseUrl() + "/" + orderNumber, OrderResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderNumber()).isEqualTo(orderNumber);
        assertThat(response.getBody().getCustomerName()).isEqualTo("Specific User");
        assertThat(response.getBody().getProductName()).isEqualTo("Specific Product");
    }

    private void testOrderStatusTransition(String orderNumber, String action, OrderStatus expectedStatus) {
        // When
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                getBaseUrl() + "/" + orderNumber + "/" + action, HttpMethod.PUT, null, OrderResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus);

        // Verify database state
        Optional<Order> updatedOrder = orderRepository.findByOrderNumber(orderNumber);
        assertThat(updatedOrder).isPresent();
        assertThat(updatedOrder.get().getStatus()).isEqualTo(expectedStatus);
    }

    private void createOrderForCustomer(String productName, String customerEmail) {
        OrderRequest request = createOrderRequest("Customer", customerEmail, productName, 1, new BigDecimal("100.00"));
        restTemplate.postForEntity(getBaseUrl(), request, OrderResponse.class);
    }

    private OrderRequest createOrderRequest(String customerName, String customerEmail, String productName, int quantity, BigDecimal unitPrice) {
        OrderRequest request = new OrderRequest();
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerEmail);
        request.setProductName(productName);
        request.setQuantity(quantity);
        request.setUnitPrice(unitPrice);
        return request;
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/orders";
    }
}