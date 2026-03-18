package ksh.tryptobackend.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class RabbitMqConfig {

    public static final String TICKER_TRADING_LISTENER_ID = "tickerTradingListener";
    public static final String TICKER_MARKETDATA_LISTENER_ID = "tickerMarketdataListener";

    @Value("${app.rabbitmq.ticker-exchange:ticker.exchange}")
    private String tickerExchangeName;

    @Bean
    public FanoutExchange tickerFanoutExchange() {
        return new FanoutExchange(tickerExchangeName, true, false);
    }

    @Bean
    public Queue tickerTradingQueue() {
        String queueName = "ticker.trading." + UUID.randomUUID().toString().substring(0, 8);
        return new Queue(queueName, false, true, true);
    }

    @Bean
    public Binding tickerTradingBinding(Queue tickerTradingQueue, FanoutExchange tickerFanoutExchange) {
        return BindingBuilder.bind(tickerTradingQueue).to(tickerFanoutExchange);
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
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
