package id.my.hendisantika.eventdrivensample.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-event-driven-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 10/08/25
 * Time: 06.35
 * To change this template use File | Settings | File Templates.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String ORDER_TOPIC = "order-events";
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishOrderEvent(OrderEvent orderEvent) {
        try {
            String eventJson = objectMapper.writeValueAsString(orderEvent);
            kafkaTemplate.send(ORDER_TOPIC, orderEvent.getOrderNumber(), eventJson);
            log.info("Published order event: {} for order: {}",
                    orderEvent.getEventType(), orderEvent.getOrderNumber());
        } catch (Exception e) {
            log.error("Error publishing order event for order: {}",
                    orderEvent.getOrderNumber(), e);
        }
    }
}