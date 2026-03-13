package ksh.tryptobackend.user.application.port.in;

import ksh.tryptobackend.user.application.port.in.dto.query.GetUserProfileQuery;
import ksh.tryptobackend.user.domain.model.User;

public interface GetUserProfileUseCase {

    User getUserProfile(GetUserProfileQuery query);
}
