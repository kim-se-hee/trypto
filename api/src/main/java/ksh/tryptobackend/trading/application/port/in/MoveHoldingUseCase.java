package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;

public interface MoveHoldingUseCase {

    void moveHolding(MoveHoldingCommand command);
}
