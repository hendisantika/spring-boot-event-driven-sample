package id.my.hendisantika.eventdrivensample.controller;

import id.my.hendisantika.eventdrivensample.dto.OrderRequest;
import id.my.hendisantika.eventdrivensample.dto.OrderResponse;
import id.my.hendisantika.eventdrivensample.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-event-driven-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 10/08/25
 * Time: 06.45
 * To change this template use File | Settings | File Templates.
 */

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
        try {
            OrderResponse orderResponse = orderService.createOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        try {
            List<OrderResponse> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching all orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            OrderResponse orderResponse = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Order not found: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/customer/{customerEmail}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerEmail(@PathVariable String customerEmail) {
        try {
            List<OrderResponse> orders = orderService.getOrdersByCustomerEmail(customerEmail);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error fetching orders for customer: {}", customerEmail, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderNumber}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderNumber) {
        try {
            OrderResponse orderResponse = orderService.confirmOrder(orderNumber);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Error confirming order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error confirming order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderNumber}/ship")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String orderNumber) {
        try {
            OrderResponse orderResponse = orderService.shipOrder(orderNumber);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Error shipping order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error shipping order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderNumber}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable String orderNumber) {
        try {
            OrderResponse orderResponse = orderService.deliverOrder(orderNumber);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Error delivering order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error delivering order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderNumber) {
        try {
            OrderResponse orderResponse = orderService.cancelOrder(orderNumber);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            log.error("Error cancelling order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error cancelling order: {}", orderNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}