package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public enum OrderMode {
    MARKET_BUY(OrderType.MARKET, Side.BUY) {
        @Override
        public InterpretedOrderInput interpret(OrderInput input, MarketInfo market) {
            input.rejectVolume();
            Money total = Money.of(input.requiredPrice());
            market.exchangeInfo().validateOrderAmount(total);
            return InterpretedOrderInput.market(Quantity.from(total.value(), market.currentPrice()));
        }

        @Override
        public BalanceChange.Lock planReservation(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return new BalanceChange.Lock(
                    pair.quoteCoinId(), settlementDebit(quantity, fill).value());
        }

        @Override
        public List<BalanceChange> planSettlementChanges(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(
                            pair.quoteCoinId(), settlementDebit(quantity, fill).value()),
                    new BalanceChange.AddAvailable(pair.tradedCoinId(), quantity.value()));
        }
    },

    MARKET_SELL(OrderType.MARKET, Side.SELL) {
        @Override
        public InterpretedOrderInput interpret(OrderInput input, MarketInfo market) {
            input.rejectPrice();
            Quantity volume = Quantity.of(input.requiredVolume());
            market.exchangeInfo().validateOrderAmount(market.currentPrice().times(volume));
            return InterpretedOrderInput.market(volume);
        }

        @Override
        public BalanceChange.Lock planReservation(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return new BalanceChange.Lock(pair.tradedCoinId(), quantity.value());
        }

        @Override
        public List<BalanceChange> planSettlementChanges(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(pair.tradedCoinId(), quantity.value()),
                    new BalanceChange.AddAvailable(
                            pair.quoteCoinId(), settlementCredit(quantity, fill).value()));
        }
    },

    LIMIT_BUY(OrderType.LIMIT, Side.BUY) {
        @Override
        public InterpretedOrderInput interpret(OrderInput input, MarketInfo market) {
            return interpretLimit(input, market);
        }

        @Override
        public boolean canFillAt(Price limitPrice, Price executionPrice) {
            return side().canFillAt(limitPrice, executionPrice);
        }

        @Override
        public BalanceChange.Lock planReservation(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return new BalanceChange.Lock(
                    pair.quoteCoinId(),
                    reservedDebit(quantity, limitPrice, feeRate).value());
        }

        @Override
        public List<BalanceChange> planSettlementChanges(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            Money reserved = reservedDebit(quantity, limitPrice, feeRate);
            Money settled = settlementDebit(quantity, fill);
            return List.of(
                    new BalanceChange.ConsumeLocked(pair.quoteCoinId(), settled.value()),
                    new BalanceChange.Unlock(
                            pair.quoteCoinId(), reserved.minus(settled).value()),
                    new BalanceChange.AddAvailable(pair.tradedCoinId(), quantity.value()));
        }
    },

    LIMIT_SELL(OrderType.LIMIT, Side.SELL) {
        @Override
        public InterpretedOrderInput interpret(OrderInput input, MarketInfo market) {
            return interpretLimit(input, market);
        }

        @Override
        public boolean canFillAt(Price limitPrice, Price executionPrice) {
            return side().canFillAt(limitPrice, executionPrice);
        }

        @Override
        public BalanceChange.Lock planReservation(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return new BalanceChange.Lock(pair.tradedCoinId(), quantity.value());
        }

        @Override
        public List<BalanceChange> planSettlementChanges(
                Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(pair.tradedCoinId(), quantity.value()),
                    new BalanceChange.AddAvailable(
                            pair.quoteCoinId(), settlementCredit(quantity, fill).value()));
        }
    };

    private final OrderType orderType;
    private final Side side;

    OrderMode(OrderType orderType, Side side) {
        this.orderType = orderType;
        this.side = side;
    }

    public static OrderMode of(OrderType orderType, Side side) {
        return switch (orderType) {
            case MARKET -> side == Side.BUY ? MARKET_BUY : MARKET_SELL;
            case LIMIT -> side == Side.BUY ? LIMIT_BUY : LIMIT_SELL;
        };
    }

    public OrderType orderType() {
        return orderType;
    }

    public Side side() {
        return side;
    }

    public abstract InterpretedOrderInput interpret(OrderInput input, MarketInfo market);

    public boolean canFillAt(Price limitPrice, Price executionPrice) {
        return true;
    }

    public abstract BalanceChange.Lock planReservation(
            Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair);

    public abstract List<BalanceChange> planSettlementChanges(
            Quantity quantity, Price limitPrice, BigDecimal feeRate, Fill fill, TradingPair pair);

    private static InterpretedOrderInput interpretLimit(OrderInput input, MarketInfo market) {
        Price limitPrice = Price.of(input.requiredPrice());
        Quantity volume = Quantity.of(input.requiredVolume());
        market.exchangeInfo().validateOrderAmount(limitPrice.times(volume));
        return InterpretedOrderInput.limit(volume, limitPrice);
    }

    private static Money reservedDebit(Quantity quantity, Price limitPrice, BigDecimal feeRate) {
        Money notional = limitPrice.times(quantity);
        return notional.plus(notional.times(feeRate));
    }

    private static Money settlementDebit(Quantity quantity, Fill fill) {
        return fill.filledPrice().times(quantity).plus(fill.fee());
    }

    private static Money settlementCredit(Quantity quantity, Fill fill) {
        return fill.filledPrice().times(quantity).minus(fill.fee());
    }
}
