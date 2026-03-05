package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.EligibleRound;

import java.util.List;

public interface EligibleRoundQueryPort {

    List<EligibleRound> findAll();
}
