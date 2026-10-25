package vn.xuandat.backend.messaging.publisher;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.xuandat.backend.config.RabbitConfig;
import vn.xuandat.backend.messaging.event.UserRegisteredEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredPublisher {
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // AFTER_COMMIT giup tranh phat sk cho 1 dki bi rollback
    public void onUserRegistered(UserRegisteredEvent event) {
        // ID riêng cho lượt publish; eventId trong body/messageId giữ ổn định.
        CorrelationData correlation = new CorrelationData();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.USER_EXCHANGE,
                    RabbitConfig.USER_REGISTERED_KEY,
                    event,
                    message -> {
                        message.getMessageProperties()
                                .setMessageId(event.eventId().toString());
                        message.getMessageProperties()
                                .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    },
                    correlation
            );

            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(5, TimeUnit.SECONDS);

            if (!confirm.ack() || correlation.getReturned() != null) {
                throw new IllegalStateException("Publish bị từ chối hoặc không có route");
            }

            log.info("Published UserRegistered eventId={} userId={}",
                    event.eventId(), event.userId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Publish bị gián đoạn eventId={} userId={}",
                    event.eventId(), event.userId(), exception);
        } catch (Exception exception) {
            log.error("Publish thất bại hoặc chưa xác định eventId={} userId={}",
                    event.eventId(), event.userId(), exception);
        }
    }
}