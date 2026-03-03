package ksh.tryptobackend.transfer.application.port.out;

import ksh.tryptobackend.transfer.application.port.out.dto.TransferExchangeInfo;

public interface TransferExchangePort {

    TransferExchangeInfo getExchangeDetail(Long exchangeId);
}
