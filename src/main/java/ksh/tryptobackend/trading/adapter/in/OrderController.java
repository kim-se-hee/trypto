package ksh.tryptobackend.trading.adapter.in;

import jakarta.validation.Valid;
import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import ksh.tryptobackend.common.dto.response.CursorPageResponseDto;
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
import ksh.tryptobackend.trading.application.port.in.PlaceOrderUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryResult;
import ksh.tryptobackend.trading.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderAvailabilityUseCase getOrderAvailabilityUseCase;
    private final FindOrderHistoryUseCase findOrderHistoryUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<PlaceOrderResponse> createOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = placeOrderUseCase.placeOrder(request.toCommand());
        return ApiResponseDto.created("주문이 처리되었습니다.", PlaceOrderResponse.from(order));
    }

    @GetMapping("/available")
    public ApiResponseDto<OrderAvailabilityResponse> getAvailability(@Valid @ModelAttribute GetOrderAvailabilityRequest request) {
        OrderAvailabilityResult result = getOrderAvailabilityUseCase.getAvailability(request.toQuery());
        return ApiResponseDto.success("조회 성공", OrderAvailabilityResponse.from(result));
    }

    @GetMapping
    public ApiResponseDto<CursorPageResponseDto<OrderHistoryResponse>> findOrderHistory(@Valid @ModelAttribute FindOrderHistoryRequest request) {
        CursorPageResponseDto<OrderHistoryResult> result = findOrderHistoryUseCase.findOrderHistory(request.toQuery());
        CursorPageResponseDto<OrderHistoryResponse> response = CursorPageResponseDto.of(
                result.content().stream().map(OrderHistoryResponse::from).toList(),
                result.nextCursor(),
                result.hasNext());
        return ApiResponseDto.success("조회 성공", response);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponseDto<CancelOrderResponse> cancelOrder(@PathVariable Long orderId) {
        Order order = cancelOrderUseCase.cancelOrder(new CancelOrderCommand(orderId));
        return ApiResponseDto.success("주문이 취소되었습니다.", CancelOrderResponse.from(order));
    }
}
