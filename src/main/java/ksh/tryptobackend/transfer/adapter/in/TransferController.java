package ksh.tryptobackend.transfer.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.transfer.adapter.in.dto.request.TransferCoinRequest;
import ksh.tryptobackend.transfer.adapter.in.dto.response.TransferCoinResponse;
import ksh.tryptobackend.transfer.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferCoinUseCase transferCoinUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<TransferCoinResponse> createTransfer(@Valid @RequestBody TransferCoinRequest request) {
        Transfer transfer = transferCoinUseCase.transferCoin(request.toCommand());
        return ApiResponseDto.created("송금이 요청되었습니다.", TransferCoinResponse.from(transfer));
    }
}
