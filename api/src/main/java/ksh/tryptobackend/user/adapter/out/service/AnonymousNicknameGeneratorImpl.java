package ksh.tryptobackend.user.adapter.out.service;

import ksh.tryptobackend.user.adapter.out.persistence.entity.NicknameSequenceJpaEntity;
import ksh.tryptobackend.user.adapter.out.persistence.repository.NicknameSequenceJpaRepository;
import ksh.tryptobackend.user.domain.service.AnonymousNicknameGenerator;
import ksh.tryptobackend.user.domain.vo.Nickname;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnonymousNicknameGeneratorImpl implements AnonymousNicknameGenerator {

    private static final String PREFIX = "탈퇴한사용자";
    private static final String SUFFIX_FORMAT = "%04d";

    private final NicknameSequenceJpaRepository nicknameSequenceJpaRepository;

    @Override
    public Nickname generate() {
        long sequence = nicknameSequenceJpaRepository
                .save(NicknameSequenceJpaEntity.create())
                .getId();
        return Nickname.of(PREFIX + String.format(SUFFIX_FORMAT, sequence));
    }
}
