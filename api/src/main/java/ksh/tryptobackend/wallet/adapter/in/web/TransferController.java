package ksh.tryptobackend.wallet.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.exception.DuplicateRequestException;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyQueryPort;
import ksh.tryptobackend.wallet.adapter.in.dto.request.TransferCoinRequest;
import ksh.tryptobackend.wallet.adapter.in.dto.response.TransferCoinResponse;
import ksh.tryptobackend.wallet.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.wallet.domain.model.Transfer;
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
    private final IdempotencyKeyQueryPort idempotencyKeyQueryPort;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<TransferCoinResponse> createTransfer(@Valid @RequestBody TransferCoinRequest request) {
        TransferCoinResponse response;
        try {
            Transfer transfer = transferCoinUseCase.transferCoin(request.toCommand());
            response = TransferCoinResponse.from(transfer);
        } catch (DuplicateRequestException e) {
            Long transferId = idempotencyKeyQueryPort
                    .findResourceId(request.idempotencyKey().toString())
                    .orElseThrow(() ->
                            new IllegalStateException("중복 송금 요청에는 연결된 리소스 ID 가 있어야 한다: " + request.idempotencyKey()));
            response = TransferCoinResponse.duplicate(transferId);
        }
        return ApiResponseDto.created("송금이 요청되었습니다.", response);
    }
}
