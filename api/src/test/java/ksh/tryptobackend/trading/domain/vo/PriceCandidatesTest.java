package ksh.tryptobackend.trading.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCandidatesTest {

    private static final Instant T1 = Instant.parse("2026-04-21T00:00:01Z");
    private static final Instant T2 = Instant.parse("2026-04-21T00:00:02Z");
    private static final Instant T3 = Instant.parse("2026-04-21T00:00:03Z");

    @Test
    @DisplayName("BUY 주문: tick 가격이 주문가 이하인 첫 tick 을 반환한다")
    void buyMatchesFirstTickAtOrBelowOrderPrice() {
        OrphanOrder order = buyOrder(bd("100"));
        PriceCandidates candidates = new PriceCandidates(List.of(
            new PriceCandidate(T1, bd("105")),
            new PriceCandidate(T2, bd("99")),
            new PriceCandidate(T3, bd("98"))
        ));

        Optional<PriceCandidate> match = candidates.findFirstMatching(order);

        assertThat(match).isPresent();
        assertThat(match.get().price()).isEqualByComparingTo("99");
        assertThat(match.get().time()).isEqualTo(T2);
    }

    @Test
    @DisplayName("BUY 주문: 교차하는 tick 이 없으면 빈 Optional 반환")
    void buyReturnsEmptyWhenNoTickCrosses() {
        OrphanOrder order = buyOrder(bd("100"));
        PriceCandidates candidates = new PriceCandidates(List.of(
            new PriceCandidate(T1, bd("105")),
            new PriceCandidate(T2, bd("110"))
        ));

        Optional<PriceCandidate> match = candidates.findFirstMatching(order);

        assertThat(match).isEmpty();
    }

    @Test
    @DisplayName("SELL 주문: tick 가격이 주문가 이상인 첫 tick 을 반환한다")
    void sellMatchesFirstTickAtOrAboveOrderPrice() {
        OrphanOrder order = sellOrder(bd("100"));
        PriceCandidates candidates = new PriceCandidates(List.of(
            new PriceCandidate(T1, bd("95")),
            new PriceCandidate(T2, bd("101"))
        ));

        Optional<PriceCandidate> match = candidates.findFirstMatching(order);

        assertThat(match).isPresent();
        assertThat(match.get().price()).isEqualByComparingTo("101");
        assertThat(match.get().time()).isEqualTo(T2);
    }

    @Test
    @DisplayName("후보가 비어있으면 빈 Optional 반환")
    void emptyCandidatesReturnsEmpty() {
        OrphanOrder order = buyOrder(bd("100"));

        Optional<PriceCandidate> match = new PriceCandidates(List.of()).findFirstMatching(order);

        assertThat(match).isEmpty();
    }

    private OrphanOrder buyOrder(BigDecimal price) {
        return new OrphanOrder(1L, 1L, 1L, 2L, 1L,
            "upbit", "BTC/KRW", Side.BUY,
            price, bd("1"), bd("100"),
            LocalDateTime.of(2026, 4, 21, 0, 0, 0));
    }

    private OrphanOrder sellOrder(BigDecimal price) {
        return new OrphanOrder(1L, 1L, 1L, 2L, 1L,
            "upbit", "BTC/KRW", Side.SELL,
            price, bd("1"), bd("1"),
            LocalDateTime.of(2026, 4, 21, 0, 0, 0));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
