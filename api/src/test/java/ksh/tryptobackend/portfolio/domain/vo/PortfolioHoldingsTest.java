package ksh.tryptobackend.portfolio.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortfolioHoldingsTest {

    private PortfolioHolding holding(Long coinId) {
        return new PortfolioHolding(
                coinId, new BigDecimal("100"), new BigDecimal("1"), new BigDecimal("120"));
    }

    @Test
    @DisplayName("메타데이터가 있는 보유 코인만 스냅샷으로 변환한다")
    void toHoldingSnapshots_skipsHoldingsWithoutMetadata() {
        PortfolioHoldings holdings = new PortfolioHoldings(List.of(holding(1L), holding(2L)));
        CoinMetadataMap coinMetadata =
                new CoinMetadataMap(Map.of(1L, new CoinMetadata("BTC", "비트코인")));

        List<HoldingSnapshot> snapshots = holdings.toHoldingSnapshots(coinMetadata);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).coinId()).isEqualTo(1L);
        assertThat(snapshots.get(0).symbol()).isEqualTo("BTC");
    }

    @Test
    @DisplayName("모든 보유 코인의 메타데이터가 있으면 전부 변환한다")
    void toHoldingSnapshots_allWithMetadata_convertsAll() {
        PortfolioHoldings holdings = new PortfolioHoldings(List.of(holding(1L), holding(2L)));
        CoinMetadataMap coinMetadata =
                new CoinMetadataMap(
                        Map.of(
                                1L, new CoinMetadata("BTC", "비트코인"),
                                2L, new CoinMetadata("ETH", "이더리움")));

        List<HoldingSnapshot> snapshots = holdings.toHoldingSnapshots(coinMetadata);

        assertThat(snapshots).hasSize(2);
    }
}
