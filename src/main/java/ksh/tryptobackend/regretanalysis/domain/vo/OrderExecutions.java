package ksh.tryptobackend.regretanalysis.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderExecutions {

    private final Map<Long, OrderExecution> executionByOrderId;

    private OrderExecutions(Map<Long, OrderExecution> executionByOrderId) {
        this.executionByOrderId = Map.copyOf(executionByOrderId);
    }

    public static OrderExecutions of(List<OrderExecution> executions) {
        Map<Long, OrderExecution> map = executions.stream()
            .collect(Collectors.toMap(OrderExecution::orderId, e -> e));
        return new OrderExecutions(map);
    }

    public boolean contains(Long orderId) {
        return executionByOrderId.containsKey(orderId);
    }

    public OrderExecution get(Long orderId) {
        return executionByOrderId.get(orderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderExecutions that = (OrderExecutions) o;
        return Objects.equals(executionByOrderId, that.executionByOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionByOrderId);
    }
}
