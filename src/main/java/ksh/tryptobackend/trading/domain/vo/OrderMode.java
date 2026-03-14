package ksh.tryptobackend.trading.domain.vo;

import ksh.tryptobackend.trading.domain.model.Order;

import java.math.BigDecimal;
import java.util.List;

public enum OrderMode {

    MARKET_BUY {
        @Override
        public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
            return venue.baseCurrencyCoinId();
        }

        @Override
        public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
            return List.of(
                new BalanceChange.Deduct(venue.baseCurrencyCoinId(), order.getSettlementDebit()),
                new BalanceChange.Add(tradeCoinId, order.getQuantity().value()));
        }
    },

    MARKET_SELL {
        @Override
        public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
            return tradeCoinId;
        }

        @Override
        public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
            return List.of(
                new BalanceChange.Deduct(tradeCoinId, order.getQuantity().value()),
                new BalanceChange.Add(venue.baseCurrencyCoinId(), order.getSettlementCredit()));
        }
    },

    LIMIT_BUY {
        @Override
        public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
            return venue.baseCurrencyCoinId();
        }

        @Override
        public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
            return List.of(
                new BalanceChange.Lock(venue.baseCurrencyCoinId(), order.getSettlementDebit()));
        }
    },

    LIMIT_SELL {
        @Override
        public Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId) {
            return tradeCoinId;
        }

        @Override
        public List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId) {
            return List.of(
                new BalanceChange.Lock(tradeCoinId, order.getQuantity().value()));
        }
    };

    public static OrderMode of(OrderType orderType, Side side) {
        return switch (orderType) {
            case MARKET -> side == Side.BUY ? MARKET_BUY : MARKET_SELL;
            case LIMIT -> side == Side.BUY ? LIMIT_BUY : LIMIT_SELL;
        };
    }

    public abstract Long resolveBalanceCoinId(TradingVenue venue, Long tradeCoinId);

    public abstract List<BalanceChange> planBalanceChanges(Order order, TradingVenue venue, Long tradeCoinId);
}
