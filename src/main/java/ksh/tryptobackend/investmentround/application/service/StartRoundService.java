package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.StartRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundRuleCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundSeedCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundRuleResult;
import ksh.tryptobackend.investmentround.application.port.out.ExchangeInfoPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundPersistencePort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRulePersistencePort;
import ksh.tryptobackend.investmentround.application.port.out.dto.ExchangeInfo;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRule;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocation;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocations;
import ksh.tryptobackend.wallet.application.port.out.WalletPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StartRoundService implements StartRoundUseCase {

    private final InvestmentRoundPersistencePort investmentRoundPersistencePort;
    private final InvestmentRulePersistencePort investmentRulePersistencePort;
    private final ExchangeInfoPort exchangeInfoPort;
    private final WalletPort walletPort;
    private final Clock clock;

    @Override
    @Transactional
    public StartRoundResult startRound(StartRoundCommand command) {
        validateActiveRound(command.userId());
        SeedAllocations seedAllocations = resolveSeedAllocations(command.seeds());

        InvestmentRound round = createAndSaveRound(
            command.userId(), command.emergencyFundingLimit(), seedAllocations);

        List<InvestmentRule> savedRules = createAndSaveRules(
            round.getRoundId(), command.rules());

        initializeWallets(round.getRoundId(), seedAllocations);

        return toResult(round, savedRules);
    }

    private void validateActiveRound(Long userId) {
        if (investmentRoundPersistencePort.existsActiveRoundByUserId(userId)) {
            throw new CustomException(ErrorCode.ACTIVE_ROUND_EXISTS);
        }
    }

    private SeedAllocations resolveSeedAllocations(List<StartRoundSeedCommand> seeds) {
        List<SeedAllocation> allocations = seeds.stream()
            .map(this::toSeedAllocation)
            .toList();
        return SeedAllocations.of(allocations);
    }

    private SeedAllocation toSeedAllocation(StartRoundSeedCommand seed) {
        ExchangeInfo exchange = exchangeInfoPort.findById(seed.exchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return SeedAllocation.create(
            seed.exchangeId(), exchange.baseCurrencyCoinId(),
            seed.amount(), exchange.seedAmountPolicy());
    }

    private InvestmentRound createAndSaveRound(Long userId, BigDecimal emergencyFundingLimit,
                                               SeedAllocations seedAllocations) {
        long previousRoundCount = investmentRoundPersistencePort.countByUserId(userId);
        InvestmentRound round = InvestmentRound.start(
            userId, previousRoundCount, seedAllocations.totalAmount(),
            emergencyFundingLimit, LocalDateTime.now(clock));
        return investmentRoundPersistencePort.save(round);
    }

    private List<InvestmentRule> createAndSaveRules(Long roundId, List<StartRoundRuleCommand> ruleCommands) {
        List<StartRoundRuleCommand> commands = ruleCommands == null ? List.of() : ruleCommands;
        if (commands.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<InvestmentRule> rules = commands.stream()
            .map(rule -> InvestmentRule.create(roundId, rule.ruleType(), rule.thresholdValue(), now))
            .toList();
        return investmentRulePersistencePort.saveAll(rules);
    }

    private void initializeWallets(Long roundId, SeedAllocations seedAllocations) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (SeedAllocation allocation : seedAllocations.getAll()) {
            walletPort.createWalletWithBalance(
                roundId, allocation.exchangeId(), allocation.baseCurrencyCoinId(),
                allocation.amount(), now);
        }
    }

    private StartRoundResult toResult(InvestmentRound round, List<InvestmentRule> rules) {
        List<StartRoundRuleResult> ruleResults = rules.stream()
            .map(rule -> new StartRoundRuleResult(rule.getRuleId(), rule.getRuleType(), rule.getThresholdValue()))
            .toList();

        return new StartRoundResult(
            round.getRoundId(),
            round.getRoundNumber(),
            round.getStatus(),
            round.getInitialSeed(),
            round.getEmergencyFundingLimit(),
            round.getEmergencyChargeCount(),
            ruleResults,
            round.getStartedAt()
        );
    }
}
