package ksh.tryptobackend.regretanalysis.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.request.GetRegretChartRequest;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.request.GetRegretReportRequest;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.response.RegretChartResponse;
import ksh.tryptobackend.regretanalysis.adapter.in.dto.response.RegretReportResponse;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds/{roundId}/regret")
@RequiredArgsConstructor
public class RegretController {

    private final GetRegretReportUseCase getRegretReportUseCase;
    private final GetRegretChartUseCase getRegretChartUseCase;

    @GetMapping
    public ApiResponseDto<RegretReportResponse> getRegretReport(
            @PathVariable Long roundId,
            @Valid @ModelAttribute GetRegretReportRequest request) {
        RegretReportResult result = getRegretReportUseCase.getRegretReport(request.toQuery(roundId));
        return ApiResponseDto.success("투자 복기 리포트를 조회했습니다.", RegretReportResponse.from(result));
    }

    @GetMapping("/chart")
    public ApiResponseDto<RegretChartResponse> getRegretChart(
        @PathVariable Long roundId,
        @Valid @ModelAttribute GetRegretChartRequest request
    ) {
        RegretChartResult result = getRegretChartUseCase.getRegretChart(request.toQuery(roundId));
        return ApiResponseDto.success("복기 그래프 데이터를 조회했습니다.", RegretChartResponse.from(result));
    }
}
