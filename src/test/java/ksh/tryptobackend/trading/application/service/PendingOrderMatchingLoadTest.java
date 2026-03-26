package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.ResolveExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.in.MatchPendingOrdersUseCase;
import ksh.tryptobackend.trading.application.port.out.PendingOrderCacheCommandPort;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.PendingOrder;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("미체결 주문 매칭 부하 테스트")
class PendingOrderMatchingLoadTest {

    private static final String EXCHANGE = "UPBIT";
    private static final String SYMBOL = "BTC";
    private static final Long EXCHANGE_COIN_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long COIN_ID = 1L;
    private static final Long BASE_CURRENCY_COIN_ID = 2L;
    private static final BigDecimal FILL_PRICE = new BigDecimal("50000000");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");
    private static final BigDecimal ORDER_QUANTITY = new BigDecimal("0.001");
    private static final int MAX_WALLETS = 150;

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASURE_ITERATIONS = 10;
    private static final int CONSUMER_THREADS = 5;
    private static final double EVENTS_PER_SECOND = 241.0;

    @Autowired
    private MatchPendingOrdersUseCase matchPendingOrdersUseCase;

    @Autowired
    private PendingOrderCacheCommandPort pendingOrderCacheCommandPort;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private WalletBalanceJpaRepository walletBalanceJpaRepository;

    @Autowired
    private MockHoldingAdapter mockHoldingAdapter;

    @MockitoBean
    private ResolveExchangeCoinMappingUseCase resolveExchangeCoinMappingUseCase;

    @MockitoBean
    private FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    @MockitoBean
    private FindExchangeDetailUseCase findExchangeDetailUseCase;

    @MockitoBean
    private GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    private final List<Long> walletIds = new ArrayList<>();

    @BeforeAll
    void setUpOnce() {
        setupWallets();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 20, 50, 100, 150})
    @DisplayName("이벤트당 매칭 건수(N)별 처리 시간 측정")
    void measureProcessingTime(int n) {
        setupMocks();
        List<Long> measurements = new ArrayList<>();

        for (int i = 0; i < WARMUP_ITERATIONS + MEASURE_ITERATIONS; i++) {
            createPendingOrdersAndCache(n);

            long startNanos = System.nanoTime();
            matchPendingOrdersUseCase.matchOrders(EXCHANGE, SYMBOL, FILL_PRICE);
            long elapsedNanos = System.nanoTime() - startNanos;

            if (i >= WARMUP_ITERATIONS) {
                measurements.add(elapsedNanos);
            }

            resetBalances();
        }

        printResult(n, measurements);
    }

    private void setupMocks() {
        given(resolveExchangeCoinMappingUseCase.resolve(EXCHANGE, SYMBOL))
            .willReturn(Optional.of(EXCHANGE_COIN_ID));
        given(findExchangeCoinMappingUseCase.findById(EXCHANGE_COIN_ID))
            .willReturn(Optional.of(new ExchangeCoinMappingResult(EXCHANGE_COIN_ID, EXCHANGE_ID, COIN_ID)));
        given(findExchangeDetailUseCase.findExchangeDetail(EXCHANGE_ID))
            .willReturn(Optional.of(new ExchangeDetailResult("Upbit", BASE_CURRENCY_COIN_ID, true, FEE_RATE)));
        given(getWalletOwnerIdUseCase.getWalletOwnerId(anyLong()))
            .willReturn(1L);
    }

    private void setupWallets() {
        BigDecimal largeBalance = new BigDecimal("1000000000");
        for (int i = 0; i < MAX_WALLETS; i++) {
            long walletId = i + 1;
            walletBalanceJpaRepository.save(
                new WalletBalanceJpaEntity(walletId, BASE_CURRENCY_COIN_ID, BigDecimal.ZERO, largeBalance));
            walletIds.add(walletId);
        }
    }

    private void createPendingOrdersAndCache(int n) {
        BigDecimal amount = FILL_PRICE.multiply(ORDER_QUANTITY);
        BigDecimal feeAmount = amount.multiply(FEE_RATE);

        for (int i = 0; i < n; i++) {
            Long walletId = walletIds.get(i % MAX_WALLETS);
            Long orderId = savePendingBuyOrder(walletId, amount, feeAmount);

            pendingOrderCacheCommandPort.add(
                new PendingOrder(orderId, EXCHANGE_COIN_ID, Side.BUY, FILL_PRICE));
        }
    }

    private Long savePendingBuyOrder(Long walletId, BigDecimal amount, BigDecimal feeAmount) {
        Order order = Order.reconstitute(
            null, "load-test-" + System.nanoTime(), walletId, EXCHANGE_COIN_ID,
            Side.BUY, OrderType.LIMIT,
            amount, new Quantity(ORDER_QUANTITY),
            FILL_PRICE, FILL_PRICE,
            Fee.of(feeAmount, FEE_RATE),
            OrderStatus.PENDING, null,
            LocalDateTime.now(), null, null);
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        return orderJpaRepository.save(entity).getId();
    }

    private void resetBalances() {
        BigDecimal largeBalance = new BigDecimal("1000000000");
        for (Long walletId : walletIds) {
            walletBalanceJpaRepository.findByWalletIdAndCoinId(walletId, BASE_CURRENCY_COIN_ID)
                .ifPresent(balance -> {
                    balance.updateFrom(
                        ksh.tryptobackend.wallet.domain.model.WalletBalance.builder()
                            .id(balance.getId())
                            .walletId(balance.getWalletId())
                            .coinId(balance.getCoinId())
                            .available(BigDecimal.ZERO)
                            .locked(largeBalance)
                            .build());
                    walletBalanceJpaRepository.save(balance);
                });
        }
        mockHoldingAdapter.clear();
    }

    private void printResult(int n, List<Long> nanosMeasurements) {
        double budgetMs = 1000.0 * CONSUMER_THREADS / EVENTS_PER_SECOND;

        Collections.sort(nanosMeasurements);
        int size = nanosMeasurements.size();
        double p50 = nanosToMs(nanosMeasurements.get(size / 2));
        double p95 = nanosToMs(nanosMeasurements.get((int) (size * 0.95)));
        double p99 = nanosToMs(nanosMeasurements.get(Math.min((int) (size * 0.99), size - 1)));
        double min = nanosToMs(nanosMeasurements.get(0));
        double max = nanosToMs(nanosMeasurements.get(size - 1));
        double avg = nanosMeasurements.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;

        boolean pass = p95 <= budgetMs;
        long totalFills = (long) n * size;

        System.out.println();
        System.out.println("============================================================");
        System.out.printf("  N=%-4d | 큐 안정 한계: %.1fms (C=%d, 241 events/s)%n", n, budgetMs, CONSUMER_THREADS);
        System.out.println("------------------------------------------------------------");
        System.out.printf("  T_process(N)  min: %7.1fms  avg: %7.1fms  max: %7.1fms%n", min, avg, max);
        System.out.printf("                p50: %7.1fms  p95: %7.1fms  p99: %7.1fms%n", p50, p95, p99);
        System.out.println("------------------------------------------------------------");
        System.out.printf("  체결 수: %d건  |  판정: %s%n", totalFills, pass ? "✅ PASS" : "❌ FAIL");
        System.out.println("============================================================");
    }

    private double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}
