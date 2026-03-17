package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.OrderFilledEventPort;
import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderFilledEventAdapter implements OrderFilledEventPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(OrderFilledEvent event) {
        eventPublisher.publishEvent(event);
    }
}
