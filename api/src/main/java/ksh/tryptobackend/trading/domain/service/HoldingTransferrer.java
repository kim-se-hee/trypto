package ksh.tryptobackend.trading.domain.service;

import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import org.springframework.stereotype.Component;

@Component
public class HoldingTransferrer {

    public void transfer(Position source, Position destination, Quantity amount, Price acquisitionPrice) {
        source.release(amount);
        destination.receive(amount, acquisitionPrice);
    }
}
