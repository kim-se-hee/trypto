package ksh.tryptobackend.marketdata.application.port.in;

import java.time.Instant;
import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.TickResult;

public interface FindTicksUseCase {

    List<TickResult> findTicks(String exchangeName, String marketSymbol, Instant from, Instant to);
}
