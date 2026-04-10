package ksh.tryptobackend.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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

import java.util.UUID;

@Configuration
public class RabbitMqConfig {

    public static final String TICKER_MARKETDATA_LISTENER_ID = "tickerMarketdataListener";
    public static final String MATCHED_ORDERS_LISTENER_ID = "matchedOrdersListener";
    public static final String MATCHED_ORDERS_CONTAINER_FACTORY = "matchedOrdersContainerFactory";

    private static final String MATCHED_ORDERS_EXCHANGE = "matched.orders";
    private static final String MATCHED_ORDERS_DLQ = "matched.orders.dlq";

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
    public Queue matchedOrdersDlq() {
        return QueueBuilder.durable(MATCHED_ORDERS_DLQ).build();
    }

    @Bean
    public DirectExchange matchedOrdersDlx() {
        return new DirectExchange(MATCHED_ORDERS_EXCHANGE + ".dlx", true, false);
    }

    @Bean
    public Binding matchedOrdersDlqBinding(Queue matchedOrdersDlq, DirectExchange matchedOrdersDlx) {
        return BindingBuilder.bind(matchedOrdersDlq).to(matchedOrdersDlx).with(MATCHED_ORDERS_DLQ);
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
}
