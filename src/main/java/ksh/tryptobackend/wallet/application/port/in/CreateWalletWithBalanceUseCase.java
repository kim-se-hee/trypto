package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.command.CreateWalletWithBalanceCommand;

public interface CreateWalletWithBalanceUseCase {

    Long createWalletWithBalance(CreateWalletWithBalanceCommand command);
}
