package com.app.relayhook.Configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // exchange name + routing key define whihc queue to deliver
    // “Send this message to the exchange named EXCHANGE_NAME, and attach the routing key ROUTING_KEY so the exchange knows which queue to deliver it to.”

    public static final String EXECUTE_EXCHANGE = "workflow.exchange";
    public static final String EXECUTE_QUEUE = "workflow.node.execute";
    public static final String EXECUTE_ROUTING_KEY = "workflow.node.execute";


    // queues
    public static final String RETRY_QUEUE = "workflow.node.queue.retry";

    // exchange
    public static final String RETRY_EXCHANGE = "workflow.node.exchange.retry";

    // routing
    public static final String RETRY_ROUTING_KEY = "workflow.node.route.retry";

    /*
     *  Exchange + ROUTING_KEY -> Queue
     */

    @Bean
    public TopicExchange workflowExchange() {
        return new TopicExchange(EXECUTE_EXCHANGE);
    }

    @Bean
    public Queue workflowQueue() {
        return new Queue(EXECUTE_QUEUE, true); // durable
    }

    @Bean
    public Queue retryQueue(){
        return new Queue(RETRY_QUEUE, true);
    }

    @Bean
    public Binding workflowBinding(Queue workflowQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(workflowQueue).to(workflowExchange).with(EXECUTE_ROUTING_KEY);
    }

    @Bean
    public Binding retryBinding(Queue retryQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(retryQueue).to(workflowExchange).with(RETRY_ROUTING_KEY);
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
