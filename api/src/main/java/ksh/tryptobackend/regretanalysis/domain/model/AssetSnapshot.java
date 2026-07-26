package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetSnapshot {

    private final Long snapshotId;
    private final Long roundId;
    private final Long exchangeId;
    private final BigDecimal totalAsset;
    private final LocalDate snapshotDate;

    public static AssetSnapshot reconstitute(
            Long snapshotId, Long roundId, Long exchangeId, BigDecimal totalAsset, LocalDate snapshotDate) {
        return AssetSnapshot.builder()
                .snapshotId(snapshotId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalAsset(totalAsset)
                .snapshotDate(snapshotDate)
                .build();
    }
}
