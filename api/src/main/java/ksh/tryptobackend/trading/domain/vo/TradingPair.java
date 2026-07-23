package ksh.tryptobackend.trading.domain.vo;

/**
 * @param quoteScale 기축통화 소수 자릿수. 수수료 절삭 단위 (KRW=0, USDT=8)
 */
public record TradingPair(Long tradedCoinId, Long quoteCoinId, int quoteScale) {}
