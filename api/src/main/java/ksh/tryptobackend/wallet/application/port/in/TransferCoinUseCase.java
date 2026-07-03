package ksh.tryptobackend.wallet.application.port.in;

import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.domain.model.Transfer;

public interface TransferCoinUseCase {

    Transfer transferCoin(TransferCoinCommand command);
}
