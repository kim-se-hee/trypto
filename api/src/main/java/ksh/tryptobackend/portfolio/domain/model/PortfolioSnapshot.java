package ksh.tryptobackend.portfolio.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.common.domain.vo.ProfitRate;
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
    private final BigDecimal totalInvestment;
    private final BigDecimal totalInvestmentKrw;
    private final BigDecimal totalProfit;
    private final BigDecimal totalProfitRate;
    private final LocalDate snapshotDate;
    @Builder.Default private final List<SnapshotDetail> details = new ArrayList<>();

    public static PortfolioSnapshot create(
            Long userId,
            Long roundId,
            Long exchangeId,
            BigDecimal totalAsset,
            BigDecimal totalInvestment,
            KrwConversionRate conversionRate,
            LocalDate snapshotDate,
            List<SnapshotDetail> details) {
        BigDecimal totalAssetKrw = conversionRate.convert(totalAsset);
        BigDecimal totalInvestmentKrw = conversionRate.convert(totalInvestment);
        BigDecimal totalProfit = totalAsset.subtract(totalInvestment);
        BigDecimal totalProfitRate =
                ProfitRate.fromAssetChange(totalAsset, totalInvestment).value();

        return PortfolioSnapshot.builder()
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalAsset(totalAsset)
                .totalAssetKrw(totalAssetKrw)
                .totalInvestment(totalInvestment)
                .totalInvestmentKrw(totalInvestmentKrw)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfitRate)
                .snapshotDate(snapshotDate)
                .details(details)
                .build();
    }
}
