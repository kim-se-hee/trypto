package ksh.tryptobackend.wallet.adapter.out.service;

import ksh.tryptobackend.trading.application.port.in.MoveHoldingUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.service.HoldingMover;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldingMoverImpl implements HoldingMover {

    private final MoveHoldingUseCase moveHoldingUseCase;

    @Override
    public void move(Transfer transfer, Long toExchangeId) {
        moveHoldingUseCase.moveHolding(new MoveHoldingCommand(
                transfer.getFromWalletId(),
                transfer.getToWalletId(),
                toExchangeId,
                transfer.getCoinId(),
                transfer.getAmount()));
    }
}
