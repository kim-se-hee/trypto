package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.ActiveRoundInfo;

import java.util.List;

public interface ActiveRoundQueryPort {

    List<ActiveRoundInfo> findAllActiveRounds();
}
