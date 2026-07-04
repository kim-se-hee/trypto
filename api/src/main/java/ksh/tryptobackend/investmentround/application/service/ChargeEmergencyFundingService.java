package ksh.tryptobackend.investmentround.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyCommandPort;
import ksh.tryptobackend.common.idempotency.IdempotencyResourceType;
import ksh.tryptobackend.investmentround.application.port.in.ChargeEmergencyFundingUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.command.ChargeEmergencyFundingCommand;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundCommandPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.WalletQueryPort;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.service.WalletBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargeEmergencyFundingService implements ChargeEmergencyFundingUseCase {

    private final IdempotencyKeyCommandPort idempotencyKeyCommandPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final InvestmentRoundCommandPort investmentRoundCommandPort;
    private final WalletQueryPort walletQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final WalletBalanceService walletBalanceService;
    private final Clock clock;

    @Override
    @Transactional
    public InvestmentRound charge(ChargeEmergencyFundingCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        String idempotencyKey = command.idempotencyKey().toString();
        idempotencyKeyCommandPort.preempt(
                idempotencyKey, IdempotencyResourceType.EMERGENCY_FUNDING, now);

        InvestmentRound round = investmentRoundQueryPort.getByIdWithLock(command.roundId());
        round.validateOwnedBy(command.userId());
        round.chargeEmergencyFunding(command.exchangeId(), command.amount(), now);

        Long walletId = walletQueryPort.getWalletId(command.roundId(), command.exchangeId());
        Long baseCurrencyCoinId = marketDataQueryPort.getBaseCurrencyCoinId(command.exchangeId());
        walletBalanceService.addAvailable(walletId, baseCurrencyCoinId, command.amount());

        InvestmentRound saved = investmentRoundCommandPort.save(round);
        idempotencyKeyCommandPort.linkResource(idempotencyKey, saved.latestFundingId());
        return saved;
    }
}
