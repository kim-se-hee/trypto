package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.out.TradingQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrder;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrders;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import ksh.tryptobackend.trading.application.port.in.FindViolatedOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindViolatedOrdersQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolatedOrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegretAnalysisAclTradingQueryAdapter implements TradingQueryPort {

    private final FindViolatedOrdersUseCase findViolatedOrdersUseCase;

    @Override
    public ViolatedOrders findViolatedOrders(Long roundId, Long exchangeId, Long walletId) {
        FindViolatedOrdersQuery query = new FindViolatedOrdersQuery(roundId, exchangeId, walletId);
        List<ViolatedOrder> orders = findViolatedOrdersUseCase.findViolatedOrders(query).stream()
                .map(this::toViolatedOrder)
                .toList();
        return new ViolatedOrders(orders);
    }

    private ViolatedOrder toViolatedOrder(ViolatedOrderResult result) {
        List<SoldPortion> soldPortions = result.soldPortions().stream()
                .map(sp -> new SoldPortion(sp.filledPrice(), sp.quantity()))
                .toList();

        return ViolatedOrder.create(
                result.orderId(),
                result.ruleId(),
                result.ruleType(),
                TradeSide.valueOf(result.side()),
                result.filledPrice(),
                result.quantity(),
                result.amount(),
                result.exchangeCoinId(),
                result.violatedAt(),
                soldPortions);
    }
}
