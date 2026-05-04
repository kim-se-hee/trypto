package ksh.tryptobackend.portfolio.application.port.in;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;

public interface FindSnapshotInputsUseCase {

    List<SnapshotInputResult> findAllSnapshotInputs();
}
