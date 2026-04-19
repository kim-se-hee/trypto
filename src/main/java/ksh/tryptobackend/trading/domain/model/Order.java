package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingContext;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Order {

    private final Long id;
    private final String idempotencyKey;
    private final Long userId;
    private final Long walletId;
    private final Long exchangeCoinId;
    private final Long coinId;
    private final Long baseCoinId;
    private final Side side;
    private final OrderType orderType;
    private BigDecimal amount;
    private final Quantity quantity;
    private final BigDecimal price;
    private BigDecimal filledPrice;
    private Fee fee;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime filledAt;
    @Builder.Default
    private final List<RuleViolation> violations = new ArrayList<>();

    public static Order create(PlaceOrderCommand cmd, TradingContext ctx) {
        return switch (cmd.orderType()) {
            case MARKET -> cmd.side() == Side.BUY
                ? createMarketBuyOrder(cmd, ctx)
                : createMarketSellOrder(cmd, ctx);
            case LIMIT -> cmd.side() == Side.BUY
                ? createLimitBuyOrder(cmd, ctx)
                : createLimitSellOrder(cmd, ctx);
        };
    }

    public static Order reconstitute(Long id, String idempotencyKey, Long userId, Long walletId,
                                     Long exchangeCoinId, Long coinId, Long baseCoinId,
                                     Side side, OrderType orderType, BigDecimal amount, Quantity quantity,
                                     BigDecimal price, BigDecimal filledPrice, Fee fee, OrderStatus status,
                                     LocalDateTime createdAt, LocalDateTime filledAt,
                                     List<RuleViolation> violations) {
        return Order.builder()
            .id(id)
            .idempotencyKey(idempotencyKey)
            .userId(userId)
            .walletId(walletId)
            .exchangeCoinId(exchangeCoinId)
            .coinId(coinId)
            .baseCoinId(baseCoinId)
            .side(side)
            .orderType(orderType)
            .amount(amount)
            .quantity(quantity)
            .price(price)
            .filledPrice(filledPrice)
            .fee(fee)
            .status(status)
            .createdAt(createdAt)
            .filledAt(filledAt)
            .violations(violations != null ? new ArrayList<>(violations) : new ArrayList<>())
            .build();
    }


    public void addViolations(List<RuleViolation> newViolations) {
        this.violations.addAll(newViolations);
    }

    public void fill(LocalDateTime now) {
        if (!isPending()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FILLABLE);
        }
        this.status = OrderStatus.FILLED;
        this.filledAt = now;
    }

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            return;
        }
        if (!isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isMarketOrder() {
        return this.orderType == OrderType.MARKET;
    }

    public boolean isLimitOrder() {
        return this.orderType == OrderType.LIMIT;
    }

    public boolean isBuyOrder() {
        return this.side == Side.BUY;
    }

    public boolean shouldForwardToEngine() {
        return isLimitOrder() && isPending();
    }

    public Long getLockedCoinId() {
        return isBuyOrder() ? baseCoinId : coinId;
    }

    public BigDecimal getLockedAmount() {
        return isBuyOrder() ? getSettlementDebit() : quantity.value();
    }

    public boolean isOwnedBy(Long walletId) {
        return this.walletId.equals(walletId);
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isAlreadyCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public BigDecimal getFilledAmount() {
        return quantity.value().multiply(filledPrice != null ? filledPrice : price);
    }

    public BigDecimal getSettlementDebit() {
        return getFilledAmount().add(fee.amount());
    }

    public BigDecimal getSettlementCredit() {
        return getFilledAmount().subtract(fee.amount());
    }

    public void validateSufficientBalance(BigDecimal available) {
        BigDecimal required = side == Side.BUY ? getSettlementDebit() : quantity.value();
        if (required.compareTo(available) > 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    private static Order createMarketBuyOrder(PlaceOrderCommand cmd, TradingContext ctx) {
        ctx.venue().validateOrderAmount(cmd.amount());
        Quantity quantity = Quantity.fromAmountAndPrice(cmd.amount(), ctx.currentPrice());
        BigDecimal filledAmount = quantity.value().multiply(ctx.currentPrice());
        Fee fee = ctx.venue().calculateFee(filledAmount);

        return createOrder(cmd, ctx, Side.BUY, OrderType.MARKET,
            filledAmount, quantity, null, ctx.currentPrice(), fee);
    }

    private static Order createMarketSellOrder(PlaceOrderCommand cmd, TradingContext ctx) {
        BigDecimal filledAmount = cmd.amount().multiply(ctx.currentPrice());
        Fee fee = ctx.venue().calculateFee(filledAmount);

        return createOrder(cmd, ctx, Side.SELL, OrderType.MARKET,
            filledAmount, new Quantity(cmd.amount()), null, ctx.currentPrice(), fee);
    }

    private static Order createLimitBuyOrder(PlaceOrderCommand cmd, TradingContext ctx) {
        if (cmd.price() == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
        ctx.venue().validateOrderAmount(cmd.amount());
        Quantity quantity = Quantity.fromAmountAndPrice(cmd.amount(), cmd.price());
        BigDecimal filledAmount = quantity.value().multiply(cmd.price());
        Fee fee = ctx.venue().calculateFee(filledAmount);

        return createOrder(cmd, ctx, Side.BUY, OrderType.LIMIT,
            filledAmount, quantity, cmd.price(), cmd.price(), fee);
    }

    private static Order createLimitSellOrder(PlaceOrderCommand cmd, TradingContext ctx) {
        if (cmd.price() == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
        BigDecimal filledAmount = cmd.amount().multiply(cmd.price());
        Fee fee = ctx.venue().calculateFee(filledAmount);

        return createOrder(cmd, ctx, Side.SELL, OrderType.LIMIT,
            filledAmount, new Quantity(cmd.amount()), cmd.price(), cmd.price(), fee);
    }

    private static Order createOrder(PlaceOrderCommand cmd, TradingContext ctx,
                                     Side side, OrderType orderType, BigDecimal amount,
                                     Quantity quantity, BigDecimal price, BigDecimal filledPrice,
                                     Fee fee) {
        TradingVenue venue = ctx.venue();
        return Order.builder()
            .idempotencyKey(cmd.idempotencyKey())
            .userId(ctx.userId())
            .walletId(cmd.walletId())
            .exchangeCoinId(cmd.exchangeCoinId())
            .coinId(ctx.coinId())
            .baseCoinId(venue.baseCurrencyCoinId())
            .side(side)
            .orderType(orderType)
            .amount(amount)
            .quantity(quantity)
            .price(price)
            .filledPrice(filledPrice)
            .fee(fee)
            .status(orderType == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.PENDING)
            .createdAt(ctx.now())
            .filledAt(orderType == OrderType.MARKET ? ctx.now() : null)
            .build();
    }
}
