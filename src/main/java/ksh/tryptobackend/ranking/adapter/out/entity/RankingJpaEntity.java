package ksh.tryptobackend.ranking.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ranking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 10)
    private RankingPeriod period;

    @Column(name = "`rank`", nullable = false)
    private int rank;

    @Column(name = "profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal profitRate;

    @Column(name = "trade_count", nullable = false)
    private int tradeCount;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RankingJpaEntity fromDomain(Ranking ranking) {
        RankingJpaEntity entity = new RankingJpaEntity();
        entity.userId = ranking.getUserId();
        entity.roundId = ranking.getRoundId();
        entity.period = ranking.getPeriod();
        entity.rank = ranking.getRank();
        entity.profitRate = ranking.getProfitRate().value();
        entity.tradeCount = ranking.getTradeCount();
        entity.referenceDate = ranking.getReferenceDate();
        entity.createdAt = ranking.getCreatedAt();
        return entity;
    }
}
