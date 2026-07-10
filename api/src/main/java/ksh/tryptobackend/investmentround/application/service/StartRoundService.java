package ksh.tryptobackend.investmentround.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.StartRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.StartRoundCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundWalletResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.model.Rule;
import ksh.tryptobackend.investmentround.domain.service.SeedWalletProvisioner;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocation;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StartRoundService implements StartRoundUseCase {

    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final MarketDataQueryPort marketDataQueryPort;

    private final SeedWalletProvisioner seedWalletProvisioner;

    private final Clock clock;

    @Override
    @Transactional
    public StartRoundResult startRound(StartRoundCommand command) {
        if (investmentRoundCommandPort.existsActiveRoundByUserId(command.userId())) {
            throw new CustomException(ErrorCode.ACTIVE_ROUND_EXISTS);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        SeedAllocations seedAllocations = resolveSeedAllocations(command);

        InvestmentRound round =
                InvestmentRound.start(
                        command.userId(),
                        investmentRoundCommandPort.countByUserId(command.userId()),
                        seedAllocations.totalAmount(),
                        command.emergencyFundingLimit(),
                        toRules(command, now),
                        now);
        InvestmentRound savedRound = investmentRoundCommandPort.save(round);

        return StartRoundResult.from(
                savedRound, provisionWallets(savedRound.getId(), seedAllocations, now));
    }

    private SeedAllocations resolveSeedAllocations(StartRoundCommand command) {
        List<SeedAllocation> allocations =
                command.seeds().stream()
                        .map(
                                seed ->
                                        SeedAllocation.create(
                                                seed.exchangeId(),
                                                marketDataQueryPort.getSeedFundingSpec(
                                                        seed.exchangeId()),
                                                seed.amount()))
                        .toList();
        return SeedAllocations.of(allocations);
    }

    private List<Rule> toRules(StartRoundCommand command, LocalDateTime now) {
        return command.rules().stream()
                .map(rule -> Rule.create(rule.ruleType(), rule.thresholdValue(), now))
                .toList();
    }

    private List<StartRoundWalletResult> provisionWallets(
            Long roundId, SeedAllocations seedAllocations, LocalDateTime now) {
        return seedAllocations.getAll().stream()
                .map(
                        allocation ->
                                new StartRoundWalletResult(
                                        seedWalletProvisioner.provision(roundId, allocation, now),
                                        allocation.exchangeId()))
                .toList();
    }
}
