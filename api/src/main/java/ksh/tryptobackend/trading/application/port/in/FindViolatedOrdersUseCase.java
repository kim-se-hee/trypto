package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;

public interface FindViolatedOrdersUseCase {

    List<ViolatedOrderResult> findViolatedOrders(FindViolatedOrdersQuery query);
}
