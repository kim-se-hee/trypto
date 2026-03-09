package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.StartRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundRuleCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundSeedCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundRuleResult;
import ksh.tryptobackend.investmentround.application.port.out.SeedFundingSpecQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocation;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocations;
import ksh.tryptobackend.wallet.application.port.in.CreateWalletWithBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.CreateWalletWithBalanceCommand;
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

    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final SeedFundingSpecQueryPort seedFundingSpecQueryPort;
    private final CreateWalletWithBalanceUseCase createWalletWithBalanceUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public StartRoundResult startRound(StartRoundCommand command) {
        validateActiveRound(command.userId());
        SeedAllocations seedAllocations = resolveSeedAllocations(command.seeds());

        InvestmentRound round = createRound(
            command.userId(), command.emergencyFundingLimit(), seedAllocations);

        addRules(round, command.rules());

        InvestmentRound savedRound = investmentRoundCommandPort.save(round);
        initializeWallets(savedRound.getRoundId(), seedAllocations);

        return toResult(savedRound);
    }

    private void validateActiveRound(Long userId) {
        if (investmentRoundCommandPort.existsActiveRoundByUserId(userId)) {
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
        SeedFundingSpec spec = seedFundingSpecQueryPort.findById(seed.exchangeId())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return SeedAllocation.create(
            seed.exchangeId(), spec.baseCurrencyCoinId(),
            seed.amount(), spec.seedAmountPolicy());
    }

    private InvestmentRound createRound(Long userId, BigDecimal emergencyFundingLimit,
                                         SeedAllocations seedAllocations) {
        long previousRoundCount = investmentRoundCommandPort.countByUserId(userId);
        return InvestmentRound.start(
            userId, previousRoundCount, seedAllocations.totalAmount(),
            emergencyFundingLimit, LocalDateTime.now(clock));
    }

    private void addRules(InvestmentRound round, List<StartRoundRuleCommand> ruleCommands) {
        List<StartRoundRuleCommand> commands = ruleCommands == null ? List.of() : ruleCommands;
        if (commands.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<RuleSetting> rules = commands.stream()
            .map(rule -> RuleSetting.create(null, rule.ruleType(), rule.thresholdValue(), now))
            .toList();
        round.addRules(rules);
    }

    private void initializeWallets(Long roundId, SeedAllocations seedAllocations) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (SeedAllocation allocation : seedAllocations.getAll()) {
            createWalletWithBalanceUseCase.createWalletWithBalance(
                new CreateWalletWithBalanceCommand(
                    roundId, allocation.exchangeId(), allocation.baseCurrencyCoinId(),
                    allocation.amount(), now));
        }
    }

    private StartRoundResult toResult(InvestmentRound round) {
        List<StartRoundRuleResult> ruleResults = round.getRules().stream()
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
