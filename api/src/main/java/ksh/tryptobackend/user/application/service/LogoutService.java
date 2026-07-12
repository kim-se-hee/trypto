package ksh.tryptobackend.user.application.service;

import ksh.tryptobackend.user.application.port.in.LogoutUseCase;
import ksh.tryptobackend.user.application.port.out.SessionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {

    private final SessionCommandPort sessionCommandPort;

    @Override
    public void logout(String sessionId) {
        sessionCommandPort.delete(sessionId);
    }
}
