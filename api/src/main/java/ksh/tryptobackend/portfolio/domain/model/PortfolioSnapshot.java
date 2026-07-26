package ksh.tryptobackend.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.vo.KrwConversionRate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioSnapshot {

    private final Long id;
    private final Long userId;
    private final Long roundId;
    private final Long exchangeId;
    private final BigDecimal totalAsset;
    private final BigDecimal totalAssetKrw;
    private final LocalDate snapshotDate;

    @Builder.Default
    private final List<SnapshotDetail> details = new ArrayList<>();

    public static PortfolioSnapshot create(
            Long userId,
            Long roundId,
            Long exchangeId,
            BigDecimal totalAsset,
            KrwConversionRate conversionRate,
            LocalDate snapshotDate,
            List<SnapshotDetail> details) {
        return PortfolioSnapshot.builder()
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalAsset(totalAsset)
                .totalAssetKrw(conversionRate.convert(totalAsset))
                .snapshotDate(snapshotDate)
                .details(details)
                .build();
    }
}
