package ksh.tryptobackend.trading.application.service;

import ksh.tryptobackend.acceptance.MockAdapterConfiguration;
import ksh.tryptobackend.acceptance.TestContainerConfiguration;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.trading.adapter.out.dto.OrderFilledMessage;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.application.port.in.FillPendingOrderUseCase;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestContainerConfiguration.class, MockAdapterConfiguration.class})
@DisplayName("주문 체결 이벤트 통합 테스트")
class OrderFilledEventIntegrationTest {

    private static final Long WALLET_ID = 1L;
    private static final Long EXCHANGE_COIN_ID = 1L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long COIN_ID = 1L;
    private static final Long BASE_CURRENCY_COIN_ID = 2L;
    private static final Long USER_ID = 100L;
    private static final BigDecimal CURRENT_PRICE = new BigDecimal("50000");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");

    @Autowired
    private FillPendingOrderUseCase fillPendingOrderUseCase;

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

    @MockitoSpyBean
    private SimpMessagingTemplate messagingTemplate;

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

    @Test
    @DisplayName("체결 성공 시 트랜잭션 커밋 후 WebSocket으로 OrderFilledMessage가 전송된다")
    void 체결_성공시_AFTER_COMMIT_이벤트로_WebSocket_전송() {
        // given
        Long orderId = savePendingOrder();

        // when
        fillPendingOrderUseCase.fillOrder(orderId, CURRENT_PRICE);

        // then — @TransactionalEventListener(AFTER_COMMIT)에 의해 WebSocket 전송 확인
        ArgumentCaptor<OrderFilledMessage> messageCaptor = ArgumentCaptor.forClass(OrderFilledMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq(USER_ID.toString()),
            eq("/queue/events"),
            messageCaptor.capture());

        OrderFilledMessage message = messageCaptor.getValue();
        assertThat(message.eventType()).isEqualTo("ORDER_FILLED");
        assertThat(message.walletId()).isEqualTo(WALLET_ID);
        assertThat(message.orderId()).isEqualTo(orderId);
        assertThat(message.coinId()).isEqualTo(COIN_ID);
        assertThat(message.side()).isEqualTo("BUY");
    }

    @Test
    @DisplayName("이미 체결된 주문은 이벤트가 발행되지 않는다")
    void 이미_체결된_주문은_이벤트_미발행() {
        // given
        Long orderId = saveFilledOrder();

        // when
        fillPendingOrderUseCase.fillOrder(orderId, CURRENT_PRICE);

        // then
        verify(messagingTemplate, org.mockito.Mockito.never())
            .convertAndSendToUser(any(), any(), any(OrderFilledMessage.class));
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

    private Long saveFilledOrder() {
        Order order = Order.reconstitute(
            null, "idempotency-" + System.nanoTime(), USER_ID, WALLET_ID, EXCHANGE_COIN_ID, COIN_ID, BASE_CURRENCY_COIN_ID,
            Side.BUY, OrderType.LIMIT,
            new BigDecimal("100000"), new Quantity(new BigDecimal("2")),
            CURRENT_PRICE, CURRENT_PRICE,
            Fee.of(new BigDecimal("50"), FEE_RATE),
            OrderStatus.FILLED,
            LocalDateTime.now(), LocalDateTime.now(), null);
        OrderJpaEntity entity = OrderJpaEntity.fromDomain(order);
        return orderJpaRepository.save(entity).getId();
    }
}
