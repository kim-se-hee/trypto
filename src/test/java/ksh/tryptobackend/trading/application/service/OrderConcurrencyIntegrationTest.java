package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.wallet.application.port.in.GetWalletOwnerIdUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
@DisplayName("주문 동시성 통합 테스트")
class OrderConcurrencyIntegrationTest {

    private static final Long WALLET_ID = 1L;
    private static final Long EXCHANGE_COIN_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long COIN_ID = 1L;
    private static final Long BASE_CURRENCY_COIN_ID = 2L;
    private static final Long USER_ID = 1L;
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("50000");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");

    @Autowired
    private FillPendingOrderUseCase fillPendingOrderUseCase;

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @MockitoBean
    private FindExchangeCoinMappingUseCase findExchangeCoinMappingUseCase;

    @MockitoBean
    private FindExchangeDetailUseCase findExchangeDetailUseCase;

    @MockitoBean
    private ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @MockitoBean
    private GetWalletOwnerIdUseCase getWalletOwnerIdUseCase;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();

        ExchangeCoinMappingResult mapping = new ExchangeCoinMappingResult(EXCHANGE_COIN_ID, EXCHANGE_ID, COIN_ID);
        ExchangeDetailResult detail = new ExchangeDetailResult("Upbit", BASE_CURRENCY_COIN_ID, true, FEE_RATE);

        given(findExchangeCoinMappingUseCase.findById(EXCHANGE_COIN_ID)).willReturn(Optional.of(mapping));
        given(findExchangeDetailUseCase.findExchangeDetail(EXCHANGE_ID)).willReturn(Optional.of(detail));
        doNothing().when(manageWalletBalanceUseCase).unlockBalance(anyLong(), anyLong(), any());
        doNothing().when(manageWalletBalanceUseCase).deductBalance(anyLong(), anyLong(), any());
        doNothing().when(manageWalletBalanceUseCase).addBalance(anyLong(), anyLong(), any());
        given(getWalletOwnerIdUseCase.getWalletOwnerId(WALLET_ID)).willReturn(USER_ID);
    }

    @Nested
    @DisplayName("동시 체결 충돌")
    class ConcurrentFill {

        @Test
        @DisplayName("동일 주문을 두 스레드가 동시에 체결하면 CAS로 하나만 실제 체결된다")
        void 동시_fill_하나만_성공() throws InterruptedException {
            // given
            Long orderId = savePendingOrder();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger completedCount = new AtomicInteger(0);

            // when
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        fillPendingOrderUseCase.fillOrder(orderId, CURRENT_PRICE);
                        completedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            ready.await();
            start.countDown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // then — 두 스레드 모두 예외 없이 완료 (CAS 실패 측은 no-op)
            assertThat(completedCount.get()).isEqualTo(2);

            OrderJpaEntity saved = orderJpaRepository.findById(orderId).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.FILLED);
        }
    }

    @Nested
    @DisplayName("취소 + 체결 경합")
    class CancelAndFillRace {

        @Test
        @DisplayName("취소와 체결이 동시에 발생하면 CAS로 하나만 상태 변경에 성공한다")
        void 취소_체결_경합_하나만_성공() throws InterruptedException {
            // given
            Long orderId = savePendingOrder();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<String> fillResult = new AtomicReference<>("PENDING");
            AtomicReference<String> cancelResult = new AtomicReference<>("PENDING");

            // when — fill 스레드
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    fillPendingOrderUseCase.fillOrder(orderId, CURRENT_PRICE);
                    fillResult.set("SUCCESS");
                } catch (Exception e) {
                    fillResult.set("ERROR: " + e.getClass().getSimpleName());
                }
            });

            // when — cancel 스레드
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    cancelOrderUseCase.cancelOrder(new CancelOrderCommand(orderId, WALLET_ID));
                    cancelResult.set("SUCCESS");
                } catch (CustomException e) {
                    cancelResult.set("CAS_FAIL");
                } catch (Exception e) {
                    cancelResult.set("ERROR: " + e.getClass().getSimpleName());
                }
            });

            ready.await();
            start.countDown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // then — DB 상태 확인: FILLED 또는 CANCELLED 중 하나
            OrderJpaEntity saved = orderJpaRepository.findById(orderId).orElseThrow();
            assertThat(saved.getStatus()).isIn(OrderStatus.FILLED, OrderStatus.CANCELLED);

            if (saved.getStatus() == OrderStatus.FILLED) {
                // fill이 CAS 성공, cancel은 CAS 실패(ORDER_NOT_CANCELLABLE)
                assertThat(fillResult.get()).isEqualTo("SUCCESS");
                assertThat(cancelResult.get()).isEqualTo("CAS_FAIL");
            } else {
                // cancel이 CAS 성공, fill은 CAS 실패(no-op)
                assertThat(cancelResult.get()).isEqualTo("SUCCESS");
                assertThat(fillResult.get()).isEqualTo("SUCCESS"); // fill은 no-op으로 정상 완료
            }
        }
    }

    private Long savePendingOrder() {
        Order order = Order.reconstitute(
            null, "idempotency-" + System.nanoTime(), USER_ID, WALLET_ID, EXCHANGE_COIN_ID, COIN_ID, BASE_CURRENCY_COIN_ID,
            Side.BUY, OrderType.LIMIT,
            new BigDecimal("100000"), new Quantity(new BigDecimal("2")),
            CURRENT_PRICE, CURRENT_PRICE,
            Fee.of(new BigDecimal("50"), FEE_RATE),
            OrderStatus.PENDING,
            LocalDateTime.now(), null, null);
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        return orderJpaRepository.save(entity).getId();
    }
}
