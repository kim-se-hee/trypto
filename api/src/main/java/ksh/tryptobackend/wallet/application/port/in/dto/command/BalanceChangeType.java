package ksh.tryptobackend.wallet.application.port.in.dto.command;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;

public enum BalanceChangeType {
    ADD_AVAILABLE {
        @Override
        public void apply(WalletBalance balance, BigDecimal amount) {
            balance.addAvailable(amount);
        }
    },
    LOCK {
        @Override
        public void apply(WalletBalance balance, BigDecimal amount) {
            balance.lock(amount);
        }
    },
    UNLOCK {
        @Override
        public void apply(WalletBalance balance, BigDecimal amount) {
            balance.unlock(amount);
        }
    },
    CONSUME_LOCKED {
        @Override
        public void apply(WalletBalance balance, BigDecimal amount) {
            balance.consumeLocked(amount);
        }
    };

    public abstract void apply(WalletBalance balance, BigDecimal amount);
}
