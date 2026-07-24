package ksh.tryptobackend.wallet.adapter.out.service;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.application.port.in.MoveHoldingUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;
import ksh.tryptobackend.wallet.domain.service.HoldingMover;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldingMoverImpl implements HoldingMover {

    private final MoveHoldingUseCase moveHoldingUseCase;

    @Override
    public void move(Long fromWalletId, Long toWalletId, Long toExchangeId, Long coinId, BigDecimal amount) {
        moveHoldingUseCase.moveHolding(new MoveHoldingCommand(fromWalletId, toWalletId, toExchangeId, coinId, amount));
    }
}
