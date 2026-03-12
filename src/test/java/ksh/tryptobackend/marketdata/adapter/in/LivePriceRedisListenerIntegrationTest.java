package ksh.tryptobackend.marketdata.adapter.in;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.marketdata.adapter.in.dto.response.LivePriceResponse;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import ksh.tryptobackend.marketdata.adapter.out.repository.ExchangeJpaRepository;
import ksh.tryptobackend.marketdata.domain.model.ExchangeMarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class,
        LivePriceRedisListenerIntegrationTest.ExchangeDataInitializer.class})
class LivePriceRedisListenerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    void Redis_PUBLISH_메시지가_STOMP_토픽으로_전달된다() throws Exception {
        // given
        BlockingQueue<LivePriceResponse> received = new LinkedBlockingQueue<>();

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/prices.1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LivePriceResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((LivePriceResponse) payload);
            }
        });

        Thread.sleep(500);

        // when
        String message = """
                {"coinId":1,"symbol":"BTC","price":143250000,"changeRate":2.3,"timestamp":1709913600000}""";
        redisTemplate.convertAndSend("prices.1", message);

        // then
        LivePriceResponse response = received.poll(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.coinId()).isEqualTo(1L);
        assertThat(response.symbol()).isEqualTo("BTC");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("143250000"));
        assertThat(response.changeRate()).isEqualByComparingTo(new BigDecimal("2.3"));
        assertThat(response.timestamp()).isEqualTo(1709913600000L);

        session.disconnect();
    }

    @TestConfiguration
    @Order(0)
    static class ExchangeDataInitializer {

        ExchangeDataInitializer(ExchangeJpaRepository exchangeJpaRepository) {
            exchangeJpaRepository.save(
                    new ExchangeJpaEntity(1L, "Upbit", ExchangeMarketType.DOMESTIC, 1L, new BigDecimal("0.0005")));
        }
    }
}
