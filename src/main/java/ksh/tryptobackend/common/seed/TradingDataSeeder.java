package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.trading.adapter.out.entity.HoldingJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.OrderFillFailureJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.HoldingJpaRepository;
import ksh.tryptobackend.trading.adapter.out.repository.OrderFillFailureJpaRepository;
import ksh.tryptobackend.trading.adapter.out.repository.OrderJpaRepository;
import ksh.tryptobackend.trading.domain.model.Holding;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class TradingDataSeeder {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.0005");
    private static final String[] MAIN_COINS = {"BTC", "ETH", "XRP", "SOL", "DOGE", "ADA", "LINK", "DOT", "ATOM", "APT"};
    private static final Map<String, BigDecimal> COIN_PRICES = Map.of(
        "BTC", new BigDecimal("95000000"),
        "ETH", new BigDecimal("5000000"),
        "XRP", new BigDecimal("3200"),
        "SOL", new BigDecimal("280000"),
        "DOGE", new BigDecimal("500"),
        "ADA", new BigDecimal("1200"),
        "LINK", new BigDecimal("25000"),
        "DOT", new BigDecimal("12000"),
        "ATOM", new BigDecimal("15000"),
        "APT", new BigDecimal("18000")
    );

    private final OrderJpaRepository orderRepository;
    private final HoldingJpaRepository holdingRepository;
    private final OrderFillFailureJpaRepository failureRepository;

    private final Random random = new Random(42);

    @Transactional
    void seed(SeedContext ctx) {
        int orderCount = 0;
        int holdingCount = 0;

        orderCount += seedMainUserOrders(ctx);
        holdingCount += seedMainUserHoldings(ctx);
        orderCount += seedBackgroundUserOrders(ctx);
        holdingCount += seedBackgroundUserHoldings(ctx);
        seedOrderFillFailures(ctx);

        log.info("[Seed] 주문 {}건, 보유 {}건 생성 완료", orderCount, holdingCount);
    }

    private int seedMainUserOrders(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        // 김비트 - 다양한 코인, 빈번 거래, 룰 위반
        count += createOrdersForUser(ctx, "김비트", "UPBIT",
            new String[]{"BTC", "ETH", "XRP", "SOL", "DOGE"}, 15, 3, now);

        // 이더리움 - 소수 코인, 안정적
        count += createOrdersForUser(ctx, "이더리움", "BITHUMB",
            new String[]{"ETH", "BTC"}, 5, 0, now);

        // 박솔라나 - 해외 코인
        count += createOrdersForUser(ctx, "박솔라나", "BINANCE",
            new String[]{"SOL", "APT", "ATOM"}, 8, 0, now);

        // 최리플 - 3개 거래소
        count += createOrdersForUser(ctx, "최리플", "UPBIT",
            new String[]{"XRP", "BTC"}, 5, 0, now);
        count += createOrdersForUser(ctx, "최리플", "BITHUMB",
            new String[]{"XRP", "ETH"}, 4, 0, now);
        count += createOrdersForUser(ctx, "최리플", "BINANCE",
            new String[]{"XRP", "SOL"}, 4, 0, now);

        // 정도지 - 파산
        count += createOrdersForUser(ctx, "정도지", "UPBIT",
            new String[]{"DOGE"}, 10, 2, now);

        // 한에이다 - 소액 1~2건
        count += createOrdersForUser(ctx, "한에이다", "UPBIT",
            new String[]{"ADA"}, 2, 0, now);

        // 강링크 - 손실 후 회복
        count += createOrdersForUser(ctx, "강링크", "BITHUMB",
            new String[]{"LINK", "BTC"}, 8, 1, now);

        // 윤닷
        count += createOrdersForUser(ctx, "윤닷", "UPBIT",
            new String[]{"DOT", "ETH"}, 6, 0, now);

        // 송아톰 - 룰 위반 전문
        count += createOrdersForUser(ctx, "송아톰", "UPBIT",
            new String[]{"ATOM", "BTC", "ETH"}, 12, 5, now);

        // 임앱트 - 고수
        count += createOrdersForUser(ctx, "임앱트", "BINANCE",
            new String[]{"BTC", "ETH", "SOL", "APT"}, 10, 0, now);

        return count;
    }

    private int createOrdersForUser(SeedContext ctx, String nickname, String exchangeName,
                                     String[] coins, int orderCount, int violationCount,
                                     LocalDateTime now) {
        Long userId = ctx.userIdByNickname.get(nickname);
        if (userId == null) return 0;

        Long roundId = ctx.activeRoundIdByUserId.get(userId);
        if (roundId == null) return 0;

        List<Long> walletIds = ctx.walletIdsByRoundId.get(roundId);
        if (walletIds == null || walletIds.isEmpty()) return 0;

        Long exchangeId = ctx.getExchangeId(exchangeName);
        if (exchangeId == null) return 0;

        Long walletId = walletIds.stream()
            .filter(wId -> exchangeId.equals(ctx.exchangeIdByWalletId.get(wId)))
            .findFirst()
            .orElse(null);
        if (walletId == null) return 0;

        List<Long> ruleIds = ctx.ruleIdsByRoundId.getOrDefault(roundId, List.of());
        List<OrderJpaEntity> orders = new ArrayList<>();
        int violationsAdded = 0;

        for (int i = 0; i < orderCount; i++) {
            String coin = coins[i % coins.length];
            Long exchangeCoinId = ctx.getExchangeCoinId(exchangeName, coin);
            if (exchangeCoinId == null) continue;

            BigDecimal price = COIN_PRICES.getOrDefault(coin, new BigDecimal("10000"));
            BigDecimal variation = BigDecimal.ONE.add(
                new BigDecimal(random.nextDouble(-0.1, 0.1)).setScale(4, RoundingMode.HALF_UP));
            BigDecimal orderPrice = price.multiply(variation).setScale(8, RoundingMode.HALF_UP);

            BigDecimal amount = new BigDecimal(random.nextInt(100000, 2000000));
            BigDecimal quantity = amount.divide(orderPrice, 8, RoundingMode.FLOOR);
            BigDecimal feeAmount = amount.multiply(FEE_RATE).setScale(8, RoundingMode.HALF_UP);

            Side side = (i % 3 == 2) ? Side.SELL : Side.BUY;
            OrderType orderType = (i % 4 == 0) ? OrderType.LIMIT : OrderType.MARKET;
            OrderStatus status = OrderStatus.FILLED;
            LocalDateTime createdAt = now.minusDays(random.nextInt(1, 30)).minusHours(random.nextInt(0, 24));
            LocalDateTime filledAt = orderType == OrderType.MARKET ? createdAt : createdAt.plusMinutes(random.nextInt(1, 60));

            List<RuleViolation> violations = new ArrayList<>();
            if (violationsAdded < violationCount && !ruleIds.isEmpty()) {
                Long ruleId = ruleIds.get(violationsAdded % ruleIds.size());
                violations.add(new RuleViolation(ruleId, "시드 데이터 룰 위반", createdAt));
                violationsAdded++;
            }

            Order order = Order.reconstitute(
                null, UUID.randomUUID().toString(), walletId, exchangeCoinId,
                side, orderType, amount, new Quantity(quantity),
                orderType == OrderType.LIMIT ? orderPrice : null,
                orderPrice, Fee.of(feeAmount, FEE_RATE), status,
                null, createdAt, filledAt, violations
            );
            orders.add(OrderJpaEntity.fromDomain(order));
        }

        List<OrderJpaEntity> saved = orderRepository.saveAll(orders);
        saved.forEach(entity -> ctx.addOrderId(walletId, entity.getId()));
        return saved.size();
    }

    private int seedMainUserHoldings(SeedContext ctx) {
        return seedHoldingsForAllWallets(ctx, 1, 10);
    }

    private int seedBackgroundUserOrders(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        int totalCount = 0;
        String[] exchanges = {"UPBIT", "BITHUMB", "BINANCE"};

        for (int i = 11; i <= 200; i++) {
            String nickname = "투자자" + i;
            Long userId = ctx.userIdByNickname.get(nickname);
            if (userId == null) continue;

            int orderCount = random.nextInt(1, 6);
            String exchange = exchanges[random.nextInt(exchanges.length)];
            String[] coins = {MAIN_COINS[random.nextInt(MAIN_COINS.length)]};
            totalCount += createOrdersForUser(ctx, nickname, exchange, coins, orderCount, 0, now);
        }
        return totalCount;
    }

    private int seedBackgroundUserHoldings(SeedContext ctx) {
        return seedHoldingsForAllWallets(ctx, 11, 200);
    }

    private int seedHoldingsForAllWallets(SeedContext ctx, int fromUserIndex, int toUserIndex) {
        List<HoldingJpaEntity> holdings = new ArrayList<>();

        for (var entry : ctx.walletIdsByRoundId.entrySet()) {
            for (Long walletId : entry.getValue()) {
                Long exchangeId = ctx.exchangeIdByWalletId.get(walletId);
                if (exchangeId == null) continue;

                String exchangeName = ctx.exchangeIdByName.entrySet().stream()
                    .filter(e -> e.getValue().equals(exchangeId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
                if (exchangeName == null) continue;

                for (String coin : MAIN_COINS) {
                    Long coinId = ctx.getCoinId(coin);
                    Long exchangeCoinId = ctx.getExchangeCoinId(exchangeName, coin);
                    if (coinId == null || exchangeCoinId == null) continue;

                    if (random.nextDouble() < 0.3) {
                        BigDecimal price = COIN_PRICES.getOrDefault(coin, new BigDecimal("10000"));
                        BigDecimal qty = new BigDecimal(random.nextDouble(0.001, 1.0)).setScale(8, RoundingMode.HALF_UP);
                        BigDecimal buyAmount = price.multiply(qty).setScale(8, RoundingMode.HALF_UP);

                        HoldingJpaEntity entity = new HoldingJpaEntity(walletId, coinId);
                        Holding holding = Holding.builder()
                            .walletId(walletId)
                            .coinId(coinId)
                            .avgBuyPrice(price)
                            .totalQuantity(qty)
                            .totalBuyAmount(buyAmount)
                            .averagingDownCount(random.nextInt(0, 3))
                            .build();
                        entity.updateFrom(holding);
                        holdings.add(entity);
                    }
                }
            }
        }

        holdingRepository.saveAll(holdings);
        return holdings.size();
    }

    private void seedOrderFillFailures(SeedContext ctx) {
        // 정도지의 주문에 대한 체결 실패 기록
        Long userId = ctx.userIdByNickname.get("정도지");
        if (userId == null) return;

        Long roundId = ctx.activeRoundIdByUserId.get(userId);
        if (roundId == null) return;

        List<Long> walletIds = ctx.walletIdsByRoundId.getOrDefault(roundId, List.of());
        List<OrderFillFailureJpaEntity> failures = new ArrayList<>();

        for (Long walletId : walletIds) {
            List<Long> orderIds = ctx.orderIdsByWalletId.getOrDefault(walletId, List.of());
            for (int i = 0; i < Math.min(3, orderIds.size()); i++) {
                OrderFillFailure failure = OrderFillFailure.builder()
                    .orderId(orderIds.get(i))
                    .attemptedPrice(new BigDecimal("100000000"))
                    .failedAt(LocalDateTime.now().minusDays(random.nextInt(1, 15)))
                    .reason("시세 급변으로 체결 실패")
                    .resolved(true)
                    .build();
                failures.add(OrderFillFailureJpaEntity.fromDomain(failure));
            }
        }

        failureRepository.saveAll(failures);
        log.info("[Seed] 체결 실패 {}건 생성 완료", failures.size());
    }
}
