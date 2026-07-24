package ksh.tryptobackend.common.config;

import java.util.UUID;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TICKER_MARKETDATA_LISTENER_ID = "tickerMarketdataListener";
    public static final String MARKET_STATUS_LISTENER_ID = "marketStatusListener";

    @Value("${app.rabbitmq.ticker-exchange:ticker.exchange}")
    private String tickerExchangeName;

    @Value("${app.rabbitmq.market-status-exchange:market.status}")
    private String marketStatusExchangeName;

    @Value("${app.rabbitmq.market-status-queue:market.status.api}")
    private String marketStatusQueueName;

    @Value("${engine.inbox.queue:engine.inbox}")
    private String engineInboxQueue;

    @Value("${engine.publisher.fanout-exchange:order.filled.notification}")
    private String orderFilledExchange;

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
    public FanoutExchange marketStatusFanoutExchange() {
        return new FanoutExchange(marketStatusExchangeName, true, false);
    }

    @Bean
    public Queue marketStatusQueue() {
        return new Queue(marketStatusQueueName, true, false, false);
    }

    @Bean
    public Binding marketStatusBinding(Queue marketStatusQueue, FanoutExchange marketStatusFanoutExchange) {
        return BindingBuilder.bind(marketStatusQueue).to(marketStatusFanoutExchange);
    }

    @Bean
    public Queue engineInboxQueue() {
        return new Queue(engineInboxQueue, true, false, false);
    }

    @Bean
    public FanoutExchange orderFilledFanoutExchange() {
        return new FanoutExchange(orderFilledExchange, true, false);
    }

    @Bean
    public Queue engineOrderFilledQueue() {
        String queueName = "engine.filled." + UUID.randomUUID().toString().substring(0, 8);
        return new Queue(queueName, false, true, true);
    }

    @Bean
    public Binding engineOrderFilledBinding(Queue engineOrderFilledQueue, FanoutExchange orderFilledFanoutExchange) {
        return BindingBuilder.bind(engineOrderFilledQueue).to(orderFilledFanoutExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        if (connectionFactory instanceof CachingConnectionFactory ccf) {
            ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            ccf.setPublisherReturns(true);
        }
        return template;
    }
}
