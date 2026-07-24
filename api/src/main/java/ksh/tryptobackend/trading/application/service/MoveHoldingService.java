package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.MoveHoldingUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.MoveHoldingCommand;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.domain.model.Position;
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
        if (hasNoHolding(command.fromWalletId(), command.coinId())) {
            return;
        }

        Price acquisitionPrice = acquisitionPriceOf(command);
        Position source = positionCommandPort.getOrCreate(command.fromWalletId(), command.coinId());
        Position destination = positionCommandPort.getOrCreate(command.toWalletId(), command.coinId());
        holdingTransferrer.transfer(source, destination, Quantity.of(command.amount()), acquisitionPrice);

        positionCommandPort.save(source);
        positionCommandPort.save(destination);
    }

    private boolean hasNoHolding(Long walletId, Long coinId) {
        return positionCommandPort
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(position -> !position.isHolding())
                .orElse(true);
    }

    private Price acquisitionPriceOf(MoveHoldingCommand command) {
        CoinExchangeMapping mapping =
                marketQueryPort.findCoinExchangeMapping(command.toExchangeId(), List.of(command.coinId()));
        return marketQueryPort.getCurrentPrice(mapping.getExchangeCoinId(command.coinId()));
    }
}
