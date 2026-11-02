package com.ssafy.nyamnyam.global.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "nyamnyam.exchange";
    public static final String RECO_REASON_QUEUE = "reco.reason";
    public static final String RECO_REASON_ROUTING_KEY = "reco.reason";
    public static final String NOTIFICATION_QUEUE = "notification.create";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.create";

    @Bean
    public TopicExchange nyamnyamExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue recoReasonQueue() {
        return QueueBuilder.durable(RECO_REASON_QUEUE).build();
    }

    @Bean
    public Binding recoReasonBinding(Queue recoReasonQueue, TopicExchange nyamnyamExchange) {
        return BindingBuilder.bind(recoReasonQueue).to(nyamnyamExchange).with(RECO_REASON_ROUTING_KEY);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange nyamnyamExchange) {
        return BindingBuilder.bind(notificationQueue).to(nyamnyamExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(
                "com.ssafy.nyamnyam.domain.recommendation.dto.event",
                "com.ssafy.nyamnyam.domain.notification.dto");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
