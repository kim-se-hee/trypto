package ksh.tryptobackend.trading.domain.vo;

public record TradingPair(Long tradedCoinId, Long quoteCoinId) {

    public Long lockedCoinId(Side side) {
        return side == Side.BUY ? quoteCoinId : tradedCoinId;
    }
}
