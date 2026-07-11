package ksh.tryptobackend.user.domain.service;

import ksh.tryptobackend.user.domain.vo.Nickname;

public interface UniqueNicknameGenerator {

    Nickname generate();
}
