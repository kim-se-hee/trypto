package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmergencyFunding {

    private final Long fundingId;
    private final Long roundId;
    private final Long exchangeId;
    private final BigDecimal amount;
    private final UUID idempotencyKey;
    private final LocalDateTime createdAt;

    public static EmergencyFunding create(
            Long roundId,
            Long exchangeId,
            BigDecimal amount,
            UUID idempotencyKey,
            LocalDateTime createdAt) {
        return EmergencyFunding.builder()
                .roundId(roundId)
                .exchangeId(exchangeId)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .build();
    }
}
