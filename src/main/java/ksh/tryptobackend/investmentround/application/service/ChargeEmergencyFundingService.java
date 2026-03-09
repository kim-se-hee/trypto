package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.ChargeEmergencyFundingUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.ChargeEmergencyFundingCommand;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.ChargeEmergencyFundingResult;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.SeedAmountPolicy;
import ksh.tryptobackend.investmentround.domain.vo.SeedFundingSpec;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.WalletResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChargeEmergencyFundingService implements ChargeEmergencyFundingUseCase {

    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final EmergencyFundingQueryPort emergencyFundingQueryPort;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final FindWalletUseCase findWalletUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public ChargeEmergencyFundingResult chargeEmergencyFunding(ChargeEmergencyFundingCommand command) {
        InvestmentRound round = getRound(command.roundId());
        round.validateOwnedBy(command.userId());

        Optional<EmergencyFunding> existing = emergencyFundingQueryPort
            .findByRoundIdAndIdempotencyKey(command.roundId(), command.idempotencyKey());
        if (existing.isPresent()) {
            return toResult(existing.get(), round);
        }

        round.chargeEmergencyFunding(command.amount());

        Long walletId = getWalletId(command.roundId(), command.exchangeId());
        SeedFundingSpec spec = getSeedFundingSpec(command.exchangeId());

        LocalDateTime now = LocalDateTime.now(clock);
        EmergencyFunding funding = EmergencyFunding.create(
            command.roundId(), command.exchangeId(), command.amount(), command.idempotencyKey(), now);
        round.addFunding(funding);

        InvestmentRound savedRound = investmentRoundCommandPort.save(round);
        manageWalletBalanceUseCase.addBalance(walletId, spec.baseCurrencyCoinId(), command.amount());

        EmergencyFunding savedFunding = savedRound.getFundings().stream()
            .filter(f -> command.idempotencyKey().equals(f.getIdempotencyKey()))
            .findFirst()
            .orElse(funding);

        return toResult(savedFunding, savedRound);
    }

    private InvestmentRound getRound(Long roundId) {
        return investmentRoundCommandPort.findById(roundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
    }

    private Long getWalletId(Long roundId, Long exchangeId) {
        return findWalletUseCase.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(WalletResult::walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private SeedFundingSpec getSeedFundingSpec(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(this::toSeedFundingSpec)
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private SeedFundingSpec toSeedFundingSpec(ExchangeDetailResult detail) {
        return new SeedFundingSpec(
            detail.baseCurrencyCoinId(),
            detail.domestic() ? SeedAmountPolicy.DOMESTIC : SeedAmountPolicy.OVERSEAS);
    }

    private ChargeEmergencyFundingResult toResult(EmergencyFunding funding, InvestmentRound round) {
        return new ChargeEmergencyFundingResult(
            round.getRoundId(),
            funding.getExchangeId(),
            funding.getAmount(),
            round.getEmergencyChargeCount(),
            funding.getCreatedAt()
        );
    }
}
