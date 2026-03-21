package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.OrderPlacedEventPort;
import ksh.tryptobackend.trading.domain.vo.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPlacedEventAdapter implements OrderPlacedEventPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(OrderPlacedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
