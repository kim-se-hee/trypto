package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.acceptance.mock.MockHoldingAdapter;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import ksh.tryptobackend.wallet.adapter.out.repository.WalletBalanceJpaRepository;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
@DisplayName("동시 체결 잔고/홀딩 정합성 통합 테스트")
class ConcurrentFillSettlementIntegrationTest {

    private static final Long WALLET_ID = 1L;
    private static final Long EXCHANGE_COIN_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long COIN_ID = 1L;
    private static final Long BASE_CURRENCY_COIN_ID = 2L;
    private static final Long USER_ID = 100L;
    private static final BigDecimal FILL_PRICE = new BigDecimal("50000");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");

    @Autowired
    private FillPendingOrderUseCase fillPendingOrderUseCase;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private MockHoldingAdapter mockHoldingAdapter;

    @Autowired
    private WalletBalanceJpaRepository walletBalanceJpaRepository;

    @MockitoBean
    private FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    @MockitoBean
    private FindExchangeDetailUseCase findExchangeDetailUseCase;

    @MockitoBean
    private GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();
        mockHoldingAdapter.clear();
        walletBalanceJpaRepository.deleteAll();

        ExchangeCoinMappingResult mapping = new ExchangeCoinMappingResult(EXCHANGE_COIN_ID, EXCHANGE_ID, COIN_ID);
        ExchangeDetailResult detail = new ExchangeDetailResult("Upbit", BASE_CURRENCY_COIN_ID, true, FEE_RATE);

        given(findExchangeCoinMappingUseCase.findById(EXCHANGE_COIN_ID)).willReturn(Optional.of(mapping));
        given(findExchangeDetailUseCase.findExchangeDetail(EXCHANGE_ID)).willReturn(Optional.of(detail));
        given(getWalletOwnerIdUseCase.getWalletOwnerId(WALLET_ID)).willReturn(USER_ID);
    }

    @Test
    @DisplayName("서로 다른 두 매수 주문이 동시에 체결되면 잔고와 홀딩 모두 정합성이 유지된다")
    void 동시_매수_체결_잔고_홀딩_정합성() throws InterruptedException {
        // given
        BigDecimal order1Quantity = new BigDecimal("2");
        BigDecimal order2Quantity = new BigDecimal("3");
        BigDecimal order1Amount = order1Quantity.multiply(FILL_PRICE);
        BigDecimal order2Amount = order2Quantity.multiply(FILL_PRICE);
        BigDecimal order1Fee = order1Amount.multiply(FEE_RATE);
        BigDecimal order2Fee = order2Amount.multiply(FEE_RATE);
        BigDecimal order1Debit = order1Amount.add(order1Fee);
        BigDecimal order2Debit = order2Amount.add(order2Fee);

        walletBalanceJpaRepository.save(
            new WalletBalanceJpaEntity(WALLET_ID, BASE_CURRENCY_COIN_ID,
                BigDecimal.ZERO, order1Debit.add(order2Debit)));

        Long orderId1 = savePendingBuyOrder(order1Quantity, order1Amount, order1Fee);
        Long orderId2 = savePendingBuyOrder(order2Quantity, order2Amount, order2Fee);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (Long orderId : List.of(orderId1, orderId2)) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    fillPendingOrderUseCase.fillOrder(orderId, FILL_PRICE);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // then — 두 체결 모두 성공
        assertThat(exceptions)
            .as("체결 중 발생한 예외: %s", exceptions.stream()
                .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
                .toList())
            .isEmpty();
        assertThat(successCount.get()).isEqualTo(2);

        // then — 두 주문 모두 FILLED
        assertThat(orderJpaRepository.findById(orderId1).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.FILLED);
        assertThat(orderJpaRepository.findById(orderId2).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.FILLED);

        // then — 기초통화 잔고: 전부 unlock → deduct 되어 0
        WalletBalanceJpaEntity baseCurrencyBalance = walletBalanceJpaRepository
            .findByWalletIdAndCoinId(WALLET_ID, BASE_CURRENCY_COIN_ID).orElseThrow();
        assertThat(baseCurrencyBalance.getAvailable()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(baseCurrencyBalance.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);

        // then — 대상코인 잔고: 두 주문의 수량 합산
        BigDecimal expectedCoinAvailable = order1Quantity.add(order2Quantity);
        WalletBalanceJpaEntity coinBalance = walletBalanceJpaRepository
            .findByWalletIdAndCoinId(WALLET_ID, COIN_ID).orElseThrow();
        assertThat(coinBalance.getAvailable()).isEqualByComparingTo(expectedCoinAvailable);
        assertThat(coinBalance.getLocked()).isEqualByComparingTo(BigDecimal.ZERO);

        // then — 홀딩: 두 매수 반영
        BigDecimal expectedTotalQuantity = order1Quantity.add(order2Quantity);
        BigDecimal expectedTotalBuyAmount = order1Amount.add(order2Amount);
        BigDecimal expectedAvgBuyPrice = expectedTotalBuyAmount
            .divide(expectedTotalQuantity, 8, RoundingMode.FLOOR);

        Holding holding = mockHoldingAdapter.findByWalletIdAndCoinId(WALLET_ID, COIN_ID)
            .orElseThrow();
        assertThat(holding.getTotalQuantity()).isEqualByComparingTo(expectedTotalQuantity);
        assertThat(holding.getTotalBuyAmount()).isEqualByComparingTo(expectedTotalBuyAmount);
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(expectedAvgBuyPrice);
    }

    private Long savePendingBuyOrder(BigDecimal quantity, BigDecimal amount, BigDecimal feeAmount) {
        Order order = Order.reconstitute(
            null, "idempotency-" + System.nanoTime(), WALLET_ID, EXCHANGE_COIN_ID,
            Side.BUY, OrderType.LIMIT,
            amount, new Quantity(quantity),
            FILL_PRICE, FILL_PRICE,
            Fee.of(feeAmount, FEE_RATE),
            OrderStatus.PENDING, null,
            LocalDateTime.now(), null, null);
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        return orderJpaRepository.save(entity).getId();
    }
}
