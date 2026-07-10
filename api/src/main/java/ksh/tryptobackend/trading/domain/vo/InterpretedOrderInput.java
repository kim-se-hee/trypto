package ksh.tryptobackend.trading.domain.vo;

public record InterpretedOrderInput(Quantity quantity, Price limitPrice) {

    public static InterpretedOrderInput market(Quantity quantity) {
        return new InterpretedOrderInput(quantity, null);
    }

    public static InterpretedOrderInput limit(Quantity quantity, Price limitPrice) {
        return new InterpretedOrderInput(quantity, limitPrice);
    }
}
