package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.application.port.in.dto.command.ApplyMarketStatusChangeCommand;

public interface ApplyMarketStatusChangeUseCase {

    void apply(ApplyMarketStatusChangeCommand command);
}
