package ksh.tryptobackend.acceptance;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfiguration {

    @Bean
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0.30");
    }

    @Bean
    @ServiceConnection
    static RedisContainer redisContainer() {
        return new RedisContainer("redis:7.4");
    }

    @Bean
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer() {
        return new RabbitMQContainer("rabbitmq:3.13-management");
    }
}
