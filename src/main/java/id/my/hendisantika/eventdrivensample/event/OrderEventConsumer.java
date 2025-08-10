package id.my.hendisantika.eventdrivensample.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-event-driven-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 10/08/25
 * Time: 06.38
 * To change this template use File | Settings | File Templates.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "order-processing-group")
    public void handleOrderEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderNumber,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        try {
            OrderEvent orderEvent = objectMapper.readValue(eventJson, OrderEvent.class);
            log.info("Received order event: {} for order: {} from topic: {}",
                    orderEvent.getEventType(), orderNumber, topic);

            processOrderEvent(orderEvent);

        } catch (Exception e) {
            log.error("Error processing order event for order: {}", orderNumber, e);
        }
    }

    private void processOrderEvent(OrderEvent orderEvent) {
        switch (orderEvent.getEventType()) {
            case "ORDER_CREATED":
                log.info("Processing order creation for order: {}", orderEvent.getOrderNumber());
                break;
            case "ORDER_CONFIRMED":
                log.info("Processing order confirmation for order: {}", orderEvent.getOrderNumber());
                break;
            case "ORDER_SHIPPED":
                log.info("Processing order shipment for order: {}", orderEvent.getOrderNumber());
                break;
            case "ORDER_DELIVERED":
                log.info("Processing order delivery for order: {}", orderEvent.getOrderNumber());
                break;
            case "ORDER_CANCELLED":
                log.info("Processing order cancellation for order: {}", orderEvent.getOrderNumber());
                break;
            default:
                log.warn("Unknown event type: {} for order: {}",
                        orderEvent.getEventType(), orderEvent.getOrderNumber());
        }
    }
}