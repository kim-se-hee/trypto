package ksh.tryptobackend.trading.adapter.in.web;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.DuplicateRequestException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyQueryPort;
import ksh.tryptobackend.trading.adapter.in.dto.request.CancelOrderRequest;
import ksh.tryptobackend.trading.adapter.in.dto.request.FindOrderHistoryRequest;
import ksh.tryptobackend.trading.adapter.in.dto.request.GetOrderAvailabilityRequest;
import ksh.tryptobackend.trading.adapter.in.dto.request.PlaceOrderRequest;
import ksh.tryptobackend.trading.adapter.in.dto.response.CancelOrderResponse;
import ksh.tryptobackend.trading.adapter.in.dto.response.OrderAvailabilityResponse;
import ksh.tryptobackend.trading.adapter.in.dto.response.OrderHistoryResponse;
import ksh.tryptobackend.trading.adapter.in.dto.response.PlaceOrderResponse;
import ksh.tryptobackend.trading.application.port.in.CancelOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.FindOrderHistoryUseCase;
import ksh.tryptobackend.trading.application.port.in.GetOrderAvailabilityUseCase;
import ksh.tryptobackend.trading.application.port.in.GetOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryCursorResult;
import ksh.tryptobackend.trading.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final GetOrderAvailabilityUseCase getOrderAvailabilityUseCase;
    private final FindOrderHistoryUseCase findOrderHistoryUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final IdempotencyKeyQueryPort idempotencyKeyQueryPort;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<PlaceOrderResponse> createOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order order;
        try {
            order = placeOrderUseCase.placeOrder(request.toCommand());
        } catch (DuplicateRequestException e) {
            Long orderId = idempotencyKeyQueryPort
                    .findResourceId(request.clientOrderId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
            order = getOrderUseCase.getById(orderId);
        }
        String message = order.isMarketOrder() ? "주문이 체결되었습니다." : "주문이 등록되었습니다.";
        return ApiResponseDto.created(message, PlaceOrderResponse.from(order));
    }

    @GetMapping("/available")
    public ApiResponseDto<OrderAvailabilityResponse> getAvailability(
            @Valid @ModelAttribute GetOrderAvailabilityRequest request) {
        OrderAvailabilityResult result = getOrderAvailabilityUseCase.getAvailability(request.toQuery());
        return ApiResponseDto.success("조회 성공", OrderAvailabilityResponse.from(result));
    }

    @GetMapping
    public ApiResponseDto<CursorPageResponseDto<OrderHistoryResponse>> findOrderHistory(
            @Valid @ModelAttribute FindOrderHistoryRequest request) {
        OrderHistoryCursorResult result = findOrderHistoryUseCase.findOrderHistory(request.toQuery());
        CursorPageResponseDto<OrderHistoryResponse> response = CursorPageResponseDto.of(
                result.content().stream().map(OrderHistoryResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
        return ApiResponseDto.success("조회 성공", response);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponseDto<CancelOrderResponse> cancelOrder(
            @PathVariable Long orderId, @Valid @RequestBody CancelOrderRequest request) {
        Order order = cancelOrderUseCase.cancelOrder(request.toCommand(orderId));
        return ApiResponseDto.success("주문이 취소되었습니다.", CancelOrderResponse.from(order));
    }
}
