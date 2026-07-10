package ksh.tryptobackend.portfolio.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioWallet;

public interface WalletQueryPort {

    PortfolioWallet getWallet(Long walletId);

    BigDecimal getBaseCurrencyBalance(Long walletId, Long baseCurrencyCoinId);
}
