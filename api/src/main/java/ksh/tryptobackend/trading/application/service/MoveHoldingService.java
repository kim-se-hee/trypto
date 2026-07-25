package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.MoveHoldingUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.model.TransferPositions;
import ksh.tryptobackend.trading.domain.service.HoldingTransferrer;
import ksh.tryptobackend.trading.domain.vo.CoinExchangeMapping;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveHoldingService implements MoveHoldingUseCase {

    private final PositionCommandPort positionCommandPort;
    private final MarketQueryPort marketQueryPort;

    private final HoldingTransferrer holdingTransferrer;

    @Override
    @Transactional
    public void moveHolding(MoveHoldingCommand command) {
        Position sourcePosition = positionCommandPort.getOrCreate(command.fromWalletId(), command.coinId());
        if (!sourcePosition.isHolding()) {
            return;
        }

        CoinExchangeMapping mapping =
                marketQueryPort.findCoinExchangeMapping(command.toExchangeId(), List.of(command.coinId()));
        Price acquisitionPrice = marketQueryPort.getCurrentPrice(mapping.getExchangeCoinId(command.coinId()));

        TransferPositions positions = positionCommandPort.getOrCreateTransferPositionsWithLock(
                command.coinId(), command.fromWalletId(), command.toWalletId());
        holdingTransferrer.transfer(
                positions.source(), positions.destination(), Quantity.of(command.amount()), acquisitionPrice);

        positionCommandPort.saveAll(positions.toList());
    }
}
