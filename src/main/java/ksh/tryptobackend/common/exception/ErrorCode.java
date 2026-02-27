package ksh.tryptobackend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(500, "internal.server.error"),

    INSUFFICIENT_BALANCE(400, "insufficient.balance"),
    BELOW_MIN_ORDER_AMOUNT(400, "below.min.order.amount"),
    ABOVE_MAX_ORDER_AMOUNT(400, "above.max.order.amount"),
    PRICE_REQUIRED_FOR_LIMIT(400, "price.required.for.limit"),
    UNSUPPORTED_BASE_CURRENCY(400, "unsupported.base.currency"),
    ORDER_NOT_CANCELLABLE(400, "order.not.cancellable"),
    ACTIVE_ROUND_EXISTS(409, "active.round.exists"),
    INVALID_SEED_AMOUNT(400, "invalid.seed.amount"),
    INVALID_EMERGENCY_FUNDING_LIMIT(400, "invalid.emergency.funding.limit"),
    INVALID_RULE_THRESHOLD(400, "invalid.rule.threshold"),

    WALLET_NOT_FOUND(404, "wallet.not.found"),
    EXCHANGE_COIN_NOT_FOUND(404, "exchange.coin.not.found"),
    EXCHANGE_NOT_FOUND(404, "exchange.not.found"),
    ORDER_NOT_FOUND(404, "order.not.found"),
    ;

    private final int status;
    private final String messageKey;
}
