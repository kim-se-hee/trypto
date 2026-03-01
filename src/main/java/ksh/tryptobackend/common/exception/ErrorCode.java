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
    EMERGENCY_FUNDING_DISABLED(400, "emergency.funding.disabled"),
    EMERGENCY_FUNDING_CHANCE_EXHAUSTED(400, "emergency.funding.chance.exhausted"),
    INVALID_EMERGENCY_FUNDING_AMOUNT(400, "invalid.emergency.funding.amount"),

    INVALID_RANKING_PERIOD(400, "invalid.ranking.period"),
    PORTFOLIO_VIEW_NOT_ALLOWED(403, "portfolio.view.not.allowed"),
    PORTFOLIO_PRIVATE(403, "portfolio.private"),

    WALLET_NOT_FOUND(404, "wallet.not.found"),
    EXCHANGE_COIN_NOT_FOUND(404, "exchange.coin.not.found"),
    EXCHANGE_NOT_FOUND(404, "exchange.not.found"),
    ORDER_NOT_FOUND(404, "order.not.found"),
    RANKING_NOT_FOUND(404, "ranking.not.found"),
    ROUND_NOT_FOUND(404, "round.not.found"),
    ROUND_ACCESS_DENIED(403, "round.access.denied"),
    ROUND_NOT_ACTIVE(409, "round.not.active"),
    USER_NOT_FOUND(404, "user.not.found"),
    REPORT_NOT_FOUND(404, "report.not.found"),
    SNAPSHOT_NOT_FOUND(404, "snapshot.not.found"),
    ;

    private final int status;
    private final String messageKey;
}
