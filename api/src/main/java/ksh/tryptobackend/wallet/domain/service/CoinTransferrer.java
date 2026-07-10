package ksh.tryptobackend.wallet.domain.service;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.domain.model.TransferBalances;
import org.springframework.stereotype.Component;

@Component
public class CoinTransferrer {

    public void transfer(TransferBalances balances, BigDecimal amount) {
        balances.source().deductAvailable(amount);
        balances.destination().addAvailable(amount);
    }
}
