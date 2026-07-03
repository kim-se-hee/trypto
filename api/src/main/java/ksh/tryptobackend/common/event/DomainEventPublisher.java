package ksh.tryptobackend.common.event;

public interface DomainEventPublisher {

    void publish(Object event);
}
