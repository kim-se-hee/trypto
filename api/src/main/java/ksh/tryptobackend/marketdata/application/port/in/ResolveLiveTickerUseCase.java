package ksh.tryptobackend.marketdata.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ResolveLiveTickerCommand;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerBatchResult;

public interface ResolveLiveTickerUseCase {

    Optional<LiveTickerBatchResult> resolve(ResolveLiveTickerCommand command);
}
