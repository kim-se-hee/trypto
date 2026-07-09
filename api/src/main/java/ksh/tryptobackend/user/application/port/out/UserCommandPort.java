package ksh.tryptobackend.user.application.port.out;

import ksh.tryptobackend.user.domain.model.User;

public interface UserCommandPort {

    User save(User user);
}
