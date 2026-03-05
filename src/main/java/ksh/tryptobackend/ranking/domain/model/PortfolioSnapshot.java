package ksh.tryptobackend.ranking.domain.model;

import ksh.tryptobackend.ranking.domain.vo.KrwConversionRate;
import ksh.tryptobackend.ranking.domain.vo.ProfitRate;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public static PortfolioSnapshot create(Long userId, Long roundId, Long exchangeId,
                                           BigDecimal totalAsset, BigDecimal totalInvestment,
                                           KrwConversionRate conversionRate, LocalDate snapshotDate) {
        BigDecimal totalAssetKrw = conversionRate.convert(totalAsset);
        BigDecimal totalInvestmentKrw = conversionRate.convert(totalInvestment);
        BigDecimal totalProfit = totalAsset.subtract(totalInvestment);
        BigDecimal totalProfitRate = ProfitRate.fromAssetChange(totalAsset, totalInvestment).value();

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
            .build();
    }
}
