package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.ActiveRoundInfo;

import java.util.List;

public interface ActiveRoundQueryPort {

    List<ActiveRoundInfo> findAllActiveRounds();
}
