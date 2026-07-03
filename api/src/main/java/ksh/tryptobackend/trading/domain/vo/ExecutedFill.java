package ksh.tryptobackend.trading.domain.vo;

public record ExecutedFill(Side side, Price filledPrice, Quantity quantity) {

    public static ExecutedFill of(Side side, Price filledPrice, Quantity quantity) {
        return new ExecutedFill(side, filledPrice, quantity);
    }
}
