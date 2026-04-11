package ksh.tryptobackend.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.util.UUID;

@Configuration
public class RabbitMqConfig {

    public static final String TICKER_MARKETDATA_LISTENER_ID = "tickerMarketdataListener";
    public static final String MATCHED_ORDERS_LISTENER_ID = "matchedOrdersListener";
    public static final String MATCHED_ORDERS_RETRY_LISTENER_ID = "matchedOrdersRetryListener";
    public static final String MATCHED_ORDERS_CONTAINER_FACTORY = "matchedOrdersContainerFactory";
    public static final String MATCHED_ORDERS_RETRY_CONTAINER_FACTORY = "matchedOrdersRetryContainerFactory";
    public static final String MATCHED_ORDERS_MAIN_RETRY_TEMPLATE = "matchedOrdersMainRetryTemplate";
    public static final String MATCHED_ORDERS_RETRY_TIER_RETRY_TEMPLATE = "matchedOrdersRetryTierRetryTemplate";

    private static final String MATCHED_ORDERS_DLX = "matched.orders.dlx";
    private static final String MATCHED_ORDERS_DLQ = "matched.orders.dlq";
    private static final String MATCHED_ORDERS_RETRY_QUEUE = "matched.orders.retry";

    private static final String X_DELIVERY_LIMIT = "x-delivery-limit";
    private static final int DELIVERY_LIMIT = 2;

    private static final int MAIN_RETRY_MAX_ATTEMPTS = 3;
    private static final long MAIN_RETRY_INITIAL_INTERVAL_MS = 100L;
    private static final double MAIN_RETRY_MULTIPLIER = 5.0;
    private static final long MAIN_RETRY_MAX_INTERVAL_MS = 500L;

    private static final int RETRY_TIER_MAX_ATTEMPTS = 4;
    private static final long RETRY_TIER_INITIAL_INTERVAL_MS = 2_000L;
    private static final double RETRY_TIER_MULTIPLIER = 2.0;
    private static final long RETRY_TIER_MAX_INTERVAL_MS = 8_000L;

    @Value("${app.rabbitmq.ticker-exchange:ticker.exchange}")
    private String tickerExchangeName;

    @Bean
    public FanoutExchange tickerFanoutExchange() {
        return new FanoutExchange(tickerExchangeName, true, false);
    }

    @Bean
    public Queue tickerMarketdataQueue() {
        String queueName = "ticker.marketdata." + UUID.randomUUID().toString().substring(0, 8);
        return new Queue(queueName, false, true, true);
    }

    @Bean
    public Binding tickerMarketdataBinding(Queue tickerMarketdataQueue, FanoutExchange tickerFanoutExchange) {
        return BindingBuilder.bind(tickerMarketdataQueue).to(tickerFanoutExchange);
    }

    @Bean
    public FanoutExchange matchedOrdersDlx() {
        return new FanoutExchange(MATCHED_ORDERS_DLX, true, false);
    }

    @Bean
    public Queue matchedOrdersDlq() {
        return QueueBuilder.durable(MATCHED_ORDERS_DLQ)
                .quorum()
                .withArgument(X_DELIVERY_LIMIT, DELIVERY_LIMIT)
                .build();
    }

    @Bean
    public Binding matchedOrdersDlqBinding(Queue matchedOrdersDlq, FanoutExchange matchedOrdersDlx) {
        return BindingBuilder.bind(matchedOrdersDlq).to(matchedOrdersDlx);
    }

    @Bean
    public Queue matchedOrdersRetryQueue() {
        return QueueBuilder.durable(MATCHED_ORDERS_RETRY_QUEUE)
                .quorum()
                .withArgument(X_DELIVERY_LIMIT, DELIVERY_LIMIT)
                .deadLetterExchange(MATCHED_ORDERS_DLX)
                .build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean(name = MATCHED_ORDERS_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory matchedOrdersContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMissingQueuesFatal(false);
        return factory;
    }

    @Bean(name = MATCHED_ORDERS_RETRY_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory matchedOrdersRetryContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMissingQueuesFatal(false);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean(name = MATCHED_ORDERS_MAIN_RETRY_TEMPLATE)
    public RetryTemplate matchedOrdersMainRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(MAIN_RETRY_MAX_ATTEMPTS)
                .exponentialBackoff(MAIN_RETRY_INITIAL_INTERVAL_MS, MAIN_RETRY_MULTIPLIER, MAIN_RETRY_MAX_INTERVAL_MS)
                .build();
    }

    @Bean(name = MATCHED_ORDERS_RETRY_TIER_RETRY_TEMPLATE)
    public RetryTemplate matchedOrdersRetryTierRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(RETRY_TIER_MAX_ATTEMPTS)
                .exponentialBackoff(RETRY_TIER_INITIAL_INTERVAL_MS, RETRY_TIER_MULTIPLIER, RETRY_TIER_MAX_INTERVAL_MS)
                .build();
    }
}
