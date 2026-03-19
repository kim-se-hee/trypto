package ksh.tryptobackend.common.seed;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.adapter.out.entity.InvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.InvestmentRoundJpaRepository;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class InvestmentRoundDataSeeder {

    private static final BigDecimal DEFAULT_SEED = new BigDecimal("10000000");
    private static final BigDecimal LARGE_SEED = new BigDecimal("50000000");
    private static final BigDecimal EMERGENCY_LIMIT = new BigDecimal("500000");

    private final InvestmentRoundJpaRepository roundRepository;

    @Transactional
    void seed(SeedContext ctx) {
        List<InvestmentRoundJpaEntity> entities = new ArrayList<>();

        entities.addAll(createMainUserRounds(ctx));
        entities.addAll(createBackgroundUserRounds(ctx));

        List<InvestmentRoundJpaEntity> saved = roundRepository.saveAll(entities);
        registerRoundIds(ctx, saved);

        log.info("[Seed] 투자 라운드 {}건 생성 완료", saved.size());
    }

    private List<InvestmentRoundJpaEntity> createMainUserRounds(SeedContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusDays(30);
        List<InvestmentRoundJpaEntity> rounds = new ArrayList<>();

        // 1. 김비트 - 고수익, 룰 위반 다수
        rounds.add(createRound(ctx, "김비트", 1, LARGE_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-10")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("20")),
                rule(RuleType.CHASE_BUY_BAN, new BigDecimal("15")),
                rule(RuleType.OVERTRADING_LIMIT, new BigDecimal("10"))
            ),
            List.of()));

        // 2. 이더리움 - 안정적, 룰 위반 없음
        rounds.add(createRound(ctx, "이더리움", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-5")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("10"))
            ),
            List.of()));

        // 3. 박솔라나 - 해외 코인
        rounds.add(createRound(ctx, "박솔라나", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-15")),
                rule(RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("3"))
            ),
            List.of()));

        // 4. 최리플 - 3개 거래소
        rounds.add(createRound(ctx, "최리플", 1, LARGE_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-10")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("15")),
                rule(RuleType.OVERTRADING_LIMIT, new BigDecimal("15"))
            ),
            List.of()));

        // 5. 정도지 - 파산
        rounds.add(createRound(ctx, "정도지", 1, DEFAULT_SEED, BigDecimal.ZERO, 3,
            RoundStatus.BANKRUPT, monthAgo.minusDays(10), monthAgo,
            List.of(rule(RuleType.LOSS_CUT, new BigDecimal("-20"))),
            List.of()));

        // 6. 한에이다 - 최근 시작, 소액
        rounds.add(createRound(ctx, "한에이다", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, now.minusDays(3), null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-10")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("10"))
            ),
            List.of()));

        // 7. 강링크 - 긴급충전 사용
        Long linkExchangeId = ctx.getExchangeId("BITHUMB");
        List<EmergencyFunding> fundings = new ArrayList<>();
        if (linkExchangeId != null) {
            fundings.add(EmergencyFunding.builder()
                .exchangeId(linkExchangeId)
                .amount(new BigDecimal("300000"))
                .idempotencyKey(UUID.randomUUID())
                .createdAt(now.minusDays(15))
                .build());
        }
        rounds.add(createRound(ctx, "강링크", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 2,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-10")),
                rule(RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("2"))
            ),
            fundings));

        // 8. 윤닷 - 종료된 라운드 + 새 라운드
        rounds.add(createRound(ctx, "윤닷", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ENDED, monthAgo.minusDays(30), monthAgo.minusDays(5),
            List.of(rule(RuleType.LOSS_CUT, new BigDecimal("-10"))),
            List.of()));
        rounds.add(createRound(ctx, "윤닷", 2, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo.minusDays(4), null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-8")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("15")),
                rule(RuleType.CHASE_BUY_BAN, new BigDecimal("10"))
            ),
            List.of()));

        // 9. 송아톰 - 룰 위반 전문
        rounds.add(createRound(ctx, "송아톰", 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-5")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("8")),
                rule(RuleType.CHASE_BUY_BAN, new BigDecimal("10")),
                rule(RuleType.AVERAGING_DOWN_LIMIT, new BigDecimal("2")),
                rule(RuleType.OVERTRADING_LIMIT, new BigDecimal("5"))
            ),
            List.of()));

        // 10. 임앱트 - 고수
        rounds.add(createRound(ctx, "임앱트", 1, LARGE_SEED, EMERGENCY_LIMIT, 3,
            RoundStatus.ACTIVE, monthAgo, null,
            List.of(
                rule(RuleType.LOSS_CUT, new BigDecimal("-7")),
                rule(RuleType.PROFIT_TAKE, new BigDecimal("25")),
                rule(RuleType.OVERTRADING_LIMIT, new BigDecimal("8"))
            ),
            List.of()));

        return rounds;
    }

    private List<InvestmentRoundJpaEntity> createBackgroundUserRounds(SeedContext ctx) {
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<InvestmentRoundJpaEntity> rounds = new ArrayList<>();

        for (int i = 11; i <= 200; i++) {
            String nickname = "투자자" + i;
            Long userId = ctx.userIdByNickname.get(nickname);
            if (userId == null) continue;

            rounds.add(createRound(ctx, nickname, 1, DEFAULT_SEED, EMERGENCY_LIMIT, 3,
                RoundStatus.ACTIVE, monthAgo, null,
                List.of(
                    rule(RuleType.LOSS_CUT, new BigDecimal("-10")),
                    rule(RuleType.PROFIT_TAKE, new BigDecimal("15"))
                ),
                List.of()));
        }
        return rounds;
    }

    private InvestmentRoundJpaEntity createRound(SeedContext ctx, String nickname, long roundNumber,
                                                  BigDecimal seed, BigDecimal emergencyLimit,
                                                  int emergencyChargeCount, RoundStatus status,
                                                  LocalDateTime startedAt, LocalDateTime endedAt,
                                                  List<RuleSetting> rules, List<EmergencyFunding> fundings) {
        Long userId = ctx.userIdByNickname.get(nickname);
        InvestmentRound round = InvestmentRound.reconstitute(
            null, null, userId, roundNumber,
            seed, emergencyLimit, emergencyChargeCount, status,
            startedAt, endedAt, rules, fundings
        );
        return InvestmentRoundJpaEntity.fromDomain(round);
    }

    private void registerRoundIds(SeedContext ctx, List<InvestmentRoundJpaEntity> saved) {
        for (InvestmentRoundJpaEntity entity : saved) {
            if (entity.getStatus() == RoundStatus.ACTIVE) {
                ctx.addActiveRound(entity.getUserId(), entity.getId());
            }
            List<Long> ruleIds = entity.getRules().stream()
                .map(r -> r.getId())
                .toList();
            ctx.addRuleIds(entity.getId(), ruleIds);
        }
    }

    private RuleSetting rule(RuleType type, BigDecimal threshold) {
        return RuleSetting.builder()
            .ruleType(type)
            .thresholdValue(threshold)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
