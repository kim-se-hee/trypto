package ksh.tryptobackend.user.application.port.out;

public interface SessionCommandPort {

    String create(Long userId);

    void delete(String sessionId);
}
