package vn.xuandat.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String USER_EXCHANGE = "user.events";
    public static final String WELCOME_QUEUE = "email.welcome";
    public static final String USER_REGISTERED_KEY = "user.registered";

    @Bean
    DirectExchange userExchange() {
        return new DirectExchange(USER_EXCHANGE, true, false);
    }

    @Bean
    Queue welcomeQueue() {
        return QueueBuilder.durable(WELCOME_QUEUE)
                .withArgument("x-queue-type", "classic")
                .build();
    }

    @Bean
    Binding welcomeBinding() {
        return BindingBuilder.bind(welcomeQueue())
                .to(userExchange())
                .with(USER_REGISTERED_KEY);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter(
                "vn.xuandat.backend.messaging.event"
        );
    }
}
