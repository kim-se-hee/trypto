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

    @Value("${app.rabbitmq.ticker-exchange:ticker.exchange}")
    private String tickerExchangeName;

    @Bean
    public FanoutExchange tickerFanoutExchange() {
        return new FanoutExchange(tickerExchangeName, true, false);
    }

    @Bean
    public Queue tickerMatchingQueue() {
        String queueName = "ticker.matching." + UUID.randomUUID().toString().substring(0, 8);
        return new Queue(queueName, false, true, true);
    }

    @Bean
    public Binding tickerMatchingBinding(Queue tickerMatchingQueue, FanoutExchange tickerFanoutExchange) {
        return BindingBuilder.bind(tickerMatchingQueue).to(tickerFanoutExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
