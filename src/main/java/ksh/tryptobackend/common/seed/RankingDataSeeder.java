package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.common.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
class RankingDataSeeder {

    private final RankingJpaRepository rankingRepository;

    private final Random random = new Random(42);

    @Transactional
    void seed(SeedContext ctx) {
        List<RankingJpaEntity> rankings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        for (RankingPeriod period : RankingPeriod.values()) {
            List<UserProfitData> profitDataList = new ArrayList<>();

            for (var entry : ctx.activeRoundIdByUserId.entrySet()) {
                Long userId = entry.getKey();
                Long roundId = entry.getValue();

                double profitRate = random.nextGaussian() * 20;
                int tradeCount = random.nextInt(1, 30);
                profitDataList.add(new UserProfitData(userId, roundId, profitRate, tradeCount));
            }

            profitDataList.sort(Comparator.comparingDouble(d -> -d.profitRate));

            for (int rank = 0; rank < profitDataList.size(); rank++) {
                UserProfitData data = profitDataList.get(rank);
                BigDecimal profitRateValue = new BigDecimal(data.profitRate).setScale(4, RoundingMode.HALF_UP);

                Ranking ranking = Ranking.create(
                    data.userId, data.roundId, period,
                    rank + 1, ProfitRate.of(profitRateValue), data.tradeCount,
                    today, now
                );
                rankings.add(RankingJpaEntity.fromDomain(ranking));
            }
        }

        rankingRepository.saveAll(rankings);
        log.info("[Seed] 랭킹 {}건 생성 완료", rankings.size());
    }

    private record UserProfitData(Long userId, Long roundId, double profitRate, int tradeCount) {}
}
