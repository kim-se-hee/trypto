package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;

import java.util.List;

public interface FindViolatedOrdersUseCase {

    List<ViolatedOrderResult> findViolatedOrders(FindViolatedOrdersQuery query);
}
