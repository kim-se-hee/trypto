package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.IssueDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.IssueDepositAddressCommand;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangeCoinChainPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangePort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressPersistencePort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.DepositAddressChainInfo;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import ksh.tryptobackend.wallet.domain.vo.DepositTargetExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueDepositAddressService implements IssueDepositAddressUseCase {

    private final WalletQueryPort walletQueryPort;
    private final DepositAddressExchangePort exchangePort;
    private final DepositAddressExchangeCoinChainPort chainPort;
    private final DepositAddressPersistencePort depositAddressPersistencePort;

    @Override
    @Transactional
    public DepositAddress issueDepositAddress(IssueDepositAddressCommand command) {
        WalletInfo wallet = getWallet(command.walletId());
        DepositTargetExchange exchange = exchangePort.getExchange(wallet.exchangeId());
        exchange.validateTransferable(command.coinId());

        DepositAddressChainInfo chainInfo = chainPort.getExchangeCoinChain(
            wallet.exchangeId(), command.coinId(), command.chain());

        return depositAddressPersistencePort.findByWalletIdAndChain(command.walletId(), command.chain())
            .orElseGet(() -> depositAddressPersistencePort.save(
                DepositAddress.create(command.walletId(), command.chain(), chainInfo.tagRequired())));
    }

    private WalletInfo getWallet(Long walletId) {
        return walletQueryPort.findById(walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }
}
