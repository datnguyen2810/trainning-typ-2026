package vn.xuandat.backend.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.xuandat.backend.config.RabbitConfig;
import vn.xuandat.backend.messaging.event.UserRegisteredEvent;
import vn.xuandat.backend.service.WelcomeEmailService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeEmailConsumer {
    private final WelcomeEmailService welcomeEmailService;

    @RabbitListener(
            id = "welcomeEmailListener",
            queues = RabbitConfig.WELCOME_QUEUE,
            autoStartup = "${app.messaging.welcome-consumer-enabled:true}"
    )
    public void consume(UserRegisteredEvent event) {
        log.info("Received UserRegistered eventId={} userId={}",
                event.eventId(), event.userId());

        welcomeEmailService.sendWelcome(event);

        log.info("Welcome email accepted by SMTP eventId={} userId={}",
                event.eventId(), event.userId());
    }
}
