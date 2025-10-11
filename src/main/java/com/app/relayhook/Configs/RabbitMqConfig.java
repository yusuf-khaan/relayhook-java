package com.app.relayhook.Configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "workflow.exchange";
    public static final String QUEUE_NAME = "workflow.node.execute";
    public static final String ROUTING_KEY = "workflow.node.execute";

    @Bean
    public TopicExchange workflowExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue workflowQueue() {
        return new Queue(QUEUE_NAME, true); // durable
    }

    @Bean
    public Binding workflowBinding(Queue workflowQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(workflowQueue).to(workflowExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
