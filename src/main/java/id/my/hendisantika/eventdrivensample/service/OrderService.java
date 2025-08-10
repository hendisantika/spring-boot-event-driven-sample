package id.my.hendisantika.eventdrivensample.service;

import id.my.hendisantika.eventdrivensample.dto.OrderRequest;
import id.my.hendisantika.eventdrivensample.dto.OrderResponse;
import id.my.hendisantika.eventdrivensample.event.OrderEvent;
import id.my.hendisantika.eventdrivensample.event.OrderEventPublisher;
import id.my.hendisantika.eventdrivensample.model.Order;
import id.my.hendisantika.eventdrivensample.model.OrderStatus;
import id.my.hendisantika.eventdrivensample.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-event-driven-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 10/08/25
 * Time: 06.42
 * To change this template use File | Settings | File Templates.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName(orderRequest.getCustomerName());
        order.setCustomerEmail(orderRequest.getCustomerEmail());
        order.setProductName(orderRequest.getProductName());
        order.setQuantity(orderRequest.getQuantity());
        order.setUnitPrice(orderRequest.getUnitPrice());
        order.setStatus(OrderStatus.CREATED);

        Order savedOrder = orderRepository.save(order);
        log.info("Created order with number: {}", savedOrder.getOrderNumber());

        publishOrderEvent(savedOrder, "ORDER_CREATED");

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        order.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Confirmed order: {}", orderNumber);

        publishOrderEvent(updatedOrder, "ORDER_CONFIRMED");

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse shipOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order must be confirmed before shipping: " + orderNumber);
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Shipped order: {}", orderNumber);

        publishOrderEvent(updatedOrder, "ORDER_SHIPPED");

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse deliverOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Order must be shipped before delivery: " + orderNumber);
        }

        order.setStatus(OrderStatus.DELIVERED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Delivered order: {}", orderNumber);

        publishOrderEvent(updatedOrder, "ORDER_DELIVERED");

        return mapToResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel delivered order: " + orderNumber);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Cancelled order: {}", orderNumber);

        publishOrderEvent(updatedOrder, "ORDER_CANCELLED");

        return mapToResponse(updatedOrder);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomerEmail(String customerEmail) {
        List<Order> orders = orderRepository.findByCustomerEmail(customerEmail);
        return orders.stream().map(this::mapToResponse).toList();
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(this::mapToResponse).toList();
    }

    private void publishOrderEvent(Order order, String eventType) {
        OrderEvent orderEvent = new OrderEvent();
        orderEvent.setEventType(eventType);
        orderEvent.setOrderId(order.getId());
        orderEvent.setOrderNumber(order.getOrderNumber());
        orderEvent.setCustomerName(order.getCustomerName());
        orderEvent.setCustomerEmail(order.getCustomerEmail());
        orderEvent.setProductName(order.getProductName());
        orderEvent.setQuantity(order.getQuantity());
        orderEvent.setUnitPrice(order.getUnitPrice());
        orderEvent.setTotalAmount(order.getTotalAmount());
        orderEvent.setStatus(order.getStatus());
        orderEvent.setTimestamp(LocalDateTime.now());

        orderEventPublisher.publishOrderEvent(orderEvent);
    }

    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}