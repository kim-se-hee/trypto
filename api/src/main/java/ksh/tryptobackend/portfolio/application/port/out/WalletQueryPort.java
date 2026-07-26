package ksh.tryptobackend.portfolio.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioWallet;
import ksh.tryptobackend.portfolio.domain.vo.WalletSnapshots;

public interface WalletQueryPort {

    PortfolioWallet getWallet(Long walletId);

    BigDecimal getBaseCurrencyBalance(Long walletId, Long baseCurrencyCoinId);

    WalletSnapshots findWalletSnapshots(List<Long> roundIds);
}
