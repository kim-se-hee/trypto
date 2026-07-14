package ksh.tryptobackend.investmentround.domain.vo;

/**
 * 현금(원화)이 들어올 수 있는 유일한 거래소. 시드머니와 긴급 자금은 모두 이 거래소로만 들어간다.
 * 다른 거래소의 자금은 송금으로만 마련하며, 이 제약이 통화가 다른 지갑에 원화 금액이 꽂히는 것을 막는다.
 */
public final class CashInflowExchange {

    public static final String NAME = "UPBIT";

    private CashInflowExchange() {}

    public static boolean matches(String exchangeName) {
        return NAME.equals(exchangeName);
    }
}
