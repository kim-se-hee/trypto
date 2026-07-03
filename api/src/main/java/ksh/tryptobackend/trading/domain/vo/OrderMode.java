package ksh.tryptobackend.trading.domain.vo;

import java.util.List;
import ksh.tryptobackend.trading.domain.model.Order;

public enum OrderMode {
    MARKET_BUY {
        @Override
        public List<BalanceChange> planSettlementChanges(Order order, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(
                            pair.quoteCoinId(), order.getSettlementDebit().value()),
                    new BalanceChange.AddAvailable(
                            pair.tradedCoinId(), order.getQuantity().value()));
        }
    },

    MARKET_SELL {
        @Override
        public List<BalanceChange> planSettlementChanges(Order order, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(
                            pair.tradedCoinId(), order.getQuantity().value()),
                    new BalanceChange.AddAvailable(
                            pair.quoteCoinId(), order.getSettlementCredit().value()));
        }
    },

    LIMIT_BUY {
        @Override
        public List<BalanceChange> planSettlementChanges(Order order, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(
                            pair.quoteCoinId(), order.getSettlementDebit().value()),
                    new BalanceChange.Unlock(
                            pair.quoteCoinId(),
                            order.getReservedDebit().minus(order.getSettlementDebit()).value()),
                    new BalanceChange.AddAvailable(
                            pair.tradedCoinId(), order.getQuantity().value()));
        }
    },

    LIMIT_SELL {
        @Override
        public List<BalanceChange> planSettlementChanges(Order order, TradingPair pair) {
            return List.of(
                    new BalanceChange.ConsumeLocked(
                            pair.tradedCoinId(), order.getQuantity().value()),
                    new BalanceChange.AddAvailable(
                            pair.quoteCoinId(), order.getSettlementCredit().value()));
        }
    };

    public static OrderMode of(OrderType orderType, Side side) {
        return switch (orderType) {
            case MARKET -> side == Side.BUY ? MARKET_BUY : MARKET_SELL;
            case LIMIT -> side == Side.BUY ? LIMIT_BUY : LIMIT_SELL;
        };
    }

    public abstract List<BalanceChange> planSettlementChanges(Order order, TradingPair pair);
}
