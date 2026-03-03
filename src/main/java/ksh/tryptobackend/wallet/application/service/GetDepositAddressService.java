package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.GetDepositAddressUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.query.GetDepositAddressQuery;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangeCoinChainPort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressExchangePort;
import ksh.tryptobackend.wallet.application.port.out.DepositAddressPersistencePort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.application.port.out.dto.DepositAddressChainInfo;
import ksh.tryptobackend.wallet.application.port.out.dto.DepositAddressExchangeInfo;
import ksh.tryptobackend.wallet.application.port.out.dto.WalletInfo;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class GetDepositAddressService implements GetDepositAddressUseCase {

    private final WalletQueryPort walletQueryPort;
    private final DepositAddressExchangePort exchangePort;
    private final DepositAddressExchangeCoinChainPort chainPort;
    private final DepositAddressPersistencePort depositAddressPersistencePort;

    @Override
    @Transactional
    public DepositAddress getDepositAddress(GetDepositAddressQuery query) {
        WalletInfo wallet = getWallet(query.walletId());
        validateNotBaseCurrency(wallet.exchangeId(), query.coinId());
        DepositAddressChainInfo chainInfo = chainPort.getExchangeCoinChain(
            wallet.exchangeId(), query.coinId(), query.chain());

        return depositAddressPersistencePort.findByWalletIdAndChain(query.walletId(), query.chain())
            .orElseGet(() -> createDepositAddress(query.walletId(), query.chain(), chainInfo.tagRequired()));
    }

    private WalletInfo getWallet(Long walletId) {
        return walletQueryPort.findById(walletId)
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private void validateNotBaseCurrency(Long exchangeId, Long coinId) {
        DepositAddressExchangeInfo exchangeInfo = exchangePort.getExchangeDetail(exchangeId);
        if (exchangeInfo.baseCurrencyCoinId().equals(coinId)) {
            throw new CustomException(ErrorCode.BASE_CURRENCY_NOT_TRANSFERABLE);
        }
    }

    private DepositAddress createDepositAddress(Long walletId, String chain, boolean tagRequired) {
        String seed = walletId + ":" + chain;
        String address = generateHash(seed);
        String tag = tagRequired ? generateHash(seed + ":tag") : null;

        DepositAddress depositAddress = DepositAddress.create(walletId, chain, address, tag);
        return depositAddressPersistencePort.save(depositAddress);
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
