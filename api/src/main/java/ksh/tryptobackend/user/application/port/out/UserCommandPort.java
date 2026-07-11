package ksh.tryptobackend.user.application.port.out;

import java.util.function.Supplier;
import ksh.tryptobackend.user.domain.model.User;
import ksh.tryptobackend.user.domain.vo.SocialIdentity;

public interface UserCommandPort {

    User save(User user);

    User register(SocialIdentity socialIdentity, Supplier<User> newUserFactory);
}
