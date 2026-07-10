package ksh.tryptobackend.wallet.domain.service;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import org.springframework.stereotype.Component;

@Component
public class CoinTransferrer {

    public void transfer(WalletBalance source, WalletBalance destination, BigDecimal amount) {
        source.deductAvailable(amount);
        destination.addAvailable(amount);
    }
}
