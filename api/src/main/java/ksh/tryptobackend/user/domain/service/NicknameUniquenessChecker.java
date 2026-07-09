package ksh.tryptobackend.user.domain.service;

import ksh.tryptobackend.user.domain.vo.Nickname;

public interface NicknameUniquenessChecker {

    void ensureUnique(Nickname nickname);
}
