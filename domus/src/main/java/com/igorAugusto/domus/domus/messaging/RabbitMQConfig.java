package com.igorAugusto.domus.domus.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para o Domus.
 *
 * Topologia escolhida:
 *   Exchange Direct  →  Routing Key "statement.import"  →  Queue "domus.statement.import"
 *
 * Por que Direct Exchange ao invés de usar a fila diretamente?
 * - Desacopla o producer da fila: o producer só conhece o exchange e a routing key
 * - Facilita adicionar consumers extras no futuro sem mudar o producer
 * - Padrão recomendado mesmo para casos simples
 *
 * Durabilidade: exchange e fila são duráveis (persistent=true) para sobreviver
 * a reinicializações do RabbitMQ.
 */
@Configuration
public class RabbitMQConfig {

    // ── Nomes das filas e exchanges ───────────────────────────────────────────
    public static final String IMPORT_QUEUE       = "domus.statement.import";
    public static final String IMPORT_EXCHANGE    = "domus.statement.direct";
    public static final String IMPORT_ROUTING_KEY = "statement.import";

    // ── Beans: fila, exchange, binding ───────────────────────────────────────

    @Bean
    public Queue importQueue() {
        // durable=true → fila sobrevive ao restart do broker
        return QueueBuilder.durable(IMPORT_QUEUE).build();
    }

    @Bean
    public DirectExchange importExchange() {
        return ExchangeBuilder.directExchange(IMPORT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding importBinding(Queue importQueue, DirectExchange importExchange) {
        return BindingBuilder
                .bind(importQueue)
                .to(importExchange)
                .with(IMPORT_ROUTING_KEY);
    }

    // ── Serialização JSON ─────────────────────────────────────────────────────

    /**
     * Converte mensagens para/de JSON automaticamente.
     * Sem isso, Spring AMQP serializa objetos como bytes Java (não portável).
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
