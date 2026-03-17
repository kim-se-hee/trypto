package ksh.tryptobackend.trading.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderFillFailure {

    private final Long id;
    private final Long orderId;
    private final BigDecimal attemptedPrice;
    private final LocalDateTime failedAt;
    private final String reason;
    private boolean resolved;

    public static OrderFillFailure create(Long orderId, BigDecimal attemptedPrice,
                                          LocalDateTime failedAt, String reason) {
        return OrderFillFailure.builder()
            .orderId(orderId)
            .attemptedPrice(attemptedPrice)
            .failedAt(failedAt)
            .reason(reason)
            .resolved(false)
            .build();
    }

    public void resolve() {
        this.resolved = true;
    }
}
