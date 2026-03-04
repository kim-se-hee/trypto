package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.domain.vo.TransferSourceExchange;

public interface TransferExchangePort {

    TransferSourceExchange getExchangeDetail(Long exchangeId);
}
