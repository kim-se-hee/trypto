package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class RegretAnalysisDataSeeder {

    private final RegretReportJpaRepository reportRepository;

    @Transactional
    void seed(SeedContext ctx) {
        List<RegretReportJpaEntity> reports = new ArrayList<>();

        // 김비트 - 룰 위반 다수
        reports.addAll(createReports(ctx, "김비트", "UPBIT"));

        // 송아톰 - 룰 위반 전문
        reports.addAll(createReports(ctx, "송아톰", "UPBIT"));

        reportRepository.saveAll(reports);
        log.info("[Seed] 후회분석 리포트 {}건 생성 완료", reports.size());
    }

    private List<RegretReportJpaEntity> createReports(SeedContext ctx, String nickname, String exchangeName) {
        Long userId = ctx.userIdByNickname.get(nickname);
        if (userId == null) return List.of();

        Long roundId = ctx.activeRoundIdByUserId.get(userId);
        if (roundId == null) return List.of();

        Long exchangeId = ctx.getExchangeId(exchangeName);
        if (exchangeId == null) return List.of();

        List<Long> ruleIds = ctx.ruleIdsByRoundId.getOrDefault(roundId, List.of());
        if (ruleIds.isEmpty()) return List.of();

        List<Long> walletIds = ctx.walletIdsByRoundId.getOrDefault(roundId, List.of());
        Long walletId = walletIds.stream()
            .filter(wId -> exchangeId.equals(ctx.exchangeIdByWalletId.get(wId)))
            .findFirst()
            .orElse(null);

        List<Long> orderIds = walletId != null
            ? ctx.orderIdsByWalletId.getOrDefault(walletId, List.of())
            : List.of();

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<RuleImpact> ruleImpacts = createRuleImpacts(ruleIds);
        List<ViolationDetail> violationDetails = createViolationDetails(ruleIds, orderIds, ctx);

        int totalViolations = violationDetails.size();
        BigDecimal missedProfit = new BigDecimal("150000");
        BigDecimal actualProfitRate = new BigDecimal("5.20");
        BigDecimal ruleFollowedProfitRate = new BigDecimal("8.50");

        RegretReport report = RegretReport.reconstitute(
            null, userId, roundId, exchangeId,
            totalViolations, missedProfit,
            actualProfitRate, ruleFollowedProfitRate,
            today.minusDays(30), today, now,
            ruleImpacts, violationDetails
        );

        return List.of(RegretReportJpaEntity.fromDomain(report));
    }

    private List<RuleImpact> createRuleImpacts(List<Long> ruleIds) {
        List<RuleImpact> impacts = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ruleIds.size()); i++) {
            impacts.add(RuleImpact.reconstitute(
                null, null, ruleIds.get(i),
                2 + i,
                new BigDecimal((i + 1) * 50000),
                ImpactGap.of(new BigDecimal("1." + (i + 1) + "0"))
            ));
        }
        return impacts;
    }

    private List<ViolationDetail> createViolationDetails(List<Long> ruleIds, List<Long> orderIds, SeedContext ctx) {
        List<ViolationDetail> details = new ArrayList<>();
        Long btcCoinId = ctx.getCoinId("BTC");
        Long ethCoinId = ctx.getCoinId("ETH");

        Map<Integer, Long> coinMapping = new java.util.HashMap<>();
        if (btcCoinId != null) coinMapping.put(0, btcCoinId);
        if (ethCoinId != null) coinMapping.put(1, ethCoinId);

        for (int i = 0; i < Math.min(5, ruleIds.size()); i++) {
            Long orderId = i < orderIds.size() ? orderIds.get(i) : null;
            Long coinId = coinMapping.getOrDefault(i % coinMapping.size(), btcCoinId);
            if (coinId == null) continue;

            details.add(ViolationDetail.reconstitute(
                null, null, orderId, ruleIds.get(i % ruleIds.size()),
                coinId,
                new BigDecimal((i + 1) * 30000),
                new BigDecimal((i + 1) * -10000),
                LocalDateTime.now().minusDays(i + 1)
            ));
        }
        return details;
    }
}
