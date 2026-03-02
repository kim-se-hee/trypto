package ksh.tryptobackend.regretanalysis.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.request.GetRegretReportRequest;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.response.RegretReportResponse;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
@RequiredArgsConstructor
public class RegretController {

    private final GetRegretReportUseCase getRegretReportUseCase;

    @GetMapping("/{roundId}/regret")
    public ApiResponseDto<RegretReportResponse> getRegretReport(
            @PathVariable Long roundId,
            @Valid @ModelAttribute GetRegretReportRequest request) {
        RegretReportResult result = getRegretReportUseCase.getRegretReport(request.toQuery(roundId));
        return ApiResponseDto.success("투자 복기 리포트를 조회했습니다.", RegretReportResponse.from(result));
    }
}
