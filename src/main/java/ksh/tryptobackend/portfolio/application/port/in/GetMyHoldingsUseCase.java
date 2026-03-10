package ksh.tryptobackend.portfolio.application.port.in;

import ksh.tryptobackend.portfolio.application.port.in.dto.query.GetMyHoldingsQuery;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.MyHoldingsResult;

public interface GetMyHoldingsUseCase {

    MyHoldingsResult getMyHoldings(GetMyHoldingsQuery query);
}
