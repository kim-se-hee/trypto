package ksh.tryptobackend.transfer.application.port.in;

import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.transfer.domain.model.Transfer;

public interface TransferCoinUseCase {

    Transfer transferCoin(TransferCoinCommand command);
}
