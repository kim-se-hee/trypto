package ksh.tryptobackend.marketdata.adapter.out;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import ksh.tryptobackend.marketdata.domain.vo.Tick;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TickQueryAdapterTest {

    private static final String TOKEN = "trypto-collector-token";
    private static final String ORG = "trypto";
    private static final String BUCKET = "ticker";

    @Container
    static final GenericContainer<?> INFLUX = new GenericContainer<>(DockerImageName.parse("influxdb:2.7"))
        .withExposedPorts(8086)
        .withEnv("DOCKER_INFLUXDB_INIT_MODE", "setup")
        .withEnv("DOCKER_INFLUXDB_INIT_USERNAME", "admin")
        .withEnv("DOCKER_INFLUXDB_INIT_PASSWORD", "admin1234")
        .withEnv("DOCKER_INFLUXDB_INIT_ORG", ORG)
        .withEnv("DOCKER_INFLUXDB_INIT_BUCKET", BUCKET)
        .withEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", TOKEN)
        .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    private static InfluxDBClient client;
    private static TickQueryAdapter adapter;
    private String exchange;
    private String symbol;

    @BeforeAll
    static void setUp() {
        String url = "http://" + INFLUX.getHost() + ":" + INFLUX.getMappedPort(8086);
        client = InfluxDBClientFactory.create(url, TOKEN.toCharArray(), ORG, BUCKET);
        adapter = new TickQueryAdapter(client);
        ReflectionTestUtils.setField(adapter, "bucket", BUCKET);
    }

    @AfterAll
    static void tearDown() {
        if (client != null) client.close();
    }

    @BeforeEach
    void uniqueTagsPerTest() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        exchange = "test-" + suffix;
        symbol = "TEST-" + suffix + "/KRW";
    }

    @Test
    @DisplayName("쓴 tick 이 주어진 range 안에서 시간 오름차순으로 조회된다")
    void findTicksReturnsWrittenPointsInTimeOrder() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.HOURS);
        writePoint(exchange, symbol, bd("102.5"), base.plusSeconds(2));
        writePoint(exchange, symbol, bd("100"),   base);
        writePoint(exchange, symbol, bd("101"),   base.plusSeconds(1));

        List<Tick> ticks = adapter.findTicks(new ExchangeSymbolKey(exchange, symbol),
            base.minusSeconds(1), base.plusSeconds(10));

        assertThat(ticks).hasSize(3);
        assertThat(ticks.get(0).price()).isEqualByComparingTo("100");
        assertThat(ticks.get(1).price()).isEqualByComparingTo("101");
        assertThat(ticks.get(2).price()).isEqualByComparingTo("102.5");
    }

    @Test
    @DisplayName("range 밖 tick, 다른 exchange/symbol tick 은 제외된다")
    void filtersByRangeAndTags() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.HOURS);
        writePoint(exchange, symbol, bd("100"), base);
        writePoint(exchange, symbol, bd("200"), base.plusSeconds(100));
        writePoint("other-ex-" + UUID.randomUUID(), symbol, bd("999"), base.plusSeconds(1));
        writePoint(exchange, "OTHER-" + UUID.randomUUID() + "/KRW", bd("888"), base.plusSeconds(1));

        List<Tick> ticks = adapter.findTicks(new ExchangeSymbolKey(exchange, symbol),
            base.minusSeconds(1), base.plusSeconds(10));

        assertThat(ticks).hasSize(1);
        assertThat(ticks.get(0).price()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("해당 tag/range 에 tick 이 없으면 빈 리스트를 반환한다")
    void returnsEmptyWhenNoMatchingTicks() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        List<Tick> ticks = adapter.findTicks(new ExchangeSymbolKey(exchange, symbol),
            base.minusSeconds(10), base);

        assertThat(ticks).isEmpty();
    }

    private static void writePoint(String exchange, String symbol, BigDecimal price, Instant time) {
        Point p = Point.measurement("ticker_raw")
            .addTag("exchange", exchange)
            .addTag("symbol", symbol)
            .addField("price", price.doubleValue())
            .time(time, WritePrecision.MS);
        WriteApiBlocking write = client.getWriteApiBlocking();
        write.writePoint(BUCKET, ORG, p);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
