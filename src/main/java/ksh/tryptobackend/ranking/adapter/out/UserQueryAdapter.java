package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.adapter.out.repository.RankingUserJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.UserQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final RankingUserJpaRepository userRepository;

    @Override
    public Optional<UserInfo> findById(Long userId) {
        return userRepository.findById(userId)
            .map(entity -> new UserInfo(entity.getId(), entity.getNickname(), entity.isPortfolioPublic()));
    }
}
