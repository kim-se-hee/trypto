package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.wallet.application.port.in.IssueDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.IssueDepositAddressCommand;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressCommandPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import ksh.tryptobackend.wallet.domain.vo.DepositTargetExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class IssueDepositAddressService implements IssueDepositAddressUseCase {

    private final WalletQueryPort walletQueryPort;
    private final DepositAddressCommandPort depositAddressCommandPort;
    private final DepositAddressQueryPort depositAddressQueryPort;

    private final FindExchangeDetailUseCase findExchangeDetailUseCase;

    private final TransactionTemplate transactionTemplate;

    @Override
    public DepositAddress issueDepositAddress(IssueDepositAddressCommand command) {
        Long exchangeId = getExchangeIdByWalletId(command.walletId());
        validateTransferable(exchangeId, command.coinId());

        return depositAddressQueryPort.findByWalletIdAndCoinId(command.walletId(), command.coinId())
            .orElseGet(() -> createDepositAddress(command));
    }

    private Long getExchangeIdByWalletId(Long walletId) {
        return walletQueryPort.findById(walletId)
            .map(Wallet::getExchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private void validateTransferable(Long exchangeId, Long coinId) {
        DepositTargetExchange exchange = findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(detail -> DepositTargetExchange.of(detail.baseCurrencyCoinId(), detail.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        exchange.validateTransferable(coinId);
    }

    private DepositAddress createDepositAddress(IssueDepositAddressCommand command) {
        try {
            return transactionTemplate.execute(status ->
                depositAddressCommandPort.save(
                    DepositAddress.create(command.walletId(), command.coinId())));
        } catch (DataIntegrityViolationException e) {
            return depositAddressQueryPort.findByWalletIdAndCoinId(command.walletId(), command.coinId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONCURRENT_MODIFICATION));
        }
    }
}
