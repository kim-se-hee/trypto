package ksh.tryptobackend.portfolio.application.port.in;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;

import java.util.List;

public interface FindSnapshotInputsUseCase {

    List<SnapshotInputResult> findAllSnapshotInputs();
}
