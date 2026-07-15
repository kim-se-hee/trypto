import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../models/cursor_page.dart';
import '../../models/enums.dart';
import '../../models/order.dart';

class OrderRepository {
  const OrderRepository(this._dio);

  final Dio _dio;

  /// 201 + `code: CREATED`. `clientOrderId` 가 중복이면 서버가 기존 주문을 조회해 정상 응답한다.
  Future<PlaceOrderResponse> placeOrder(PlaceOrderRequest request) =>
      apiCall(() async {
        final response = await _dio.post('/api/orders', data: request.toJson());
        return PlaceOrderResponse.fromJson(
          response.data as Map<String, dynamic>,
        );
      });

  Future<OrderAvailability> getAvailability({
    required int walletId,
    required int exchangeCoinId,
    required Side side,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/orders/available',
      queryParameters: query({
        'walletId': walletId,
        'exchangeCoinId': exchangeCoinId,
        'side': side.wire,
      }),
    );
    return OrderAvailability.fromJson(response.data as Map<String, dynamic>);
  });

  /// 커서 이름이 `cursorOrderId` 다. 응답 항목에는 `status` 가 없다(사양서 R4-9) —
  /// [status] 는 조회 **필터**일 뿐이다.
  Future<CursorPage<OrderHistoryItem>> getOrderHistory({
    required int walletId,
    int? exchangeCoinId,
    Side? side,
    OrderStatus? status,
    int? cursorOrderId,
    int size = 20,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/orders',
      queryParameters: query({
        'walletId': walletId,
        'exchangeCoinId': exchangeCoinId,
        'side': side?.wire,
        'status': status?.wire,
        'cursorOrderId': cursorOrderId,
        'size': size,
      }),
    );
    return CursorPage.fromJson(
      response.data as Map<String, dynamic>,
      (json) => OrderHistoryItem.fromJson(json as Map<String, dynamic>),
    );
  });

  Future<CancelOrderResponse> cancelOrder({
    required int orderId,
    required int walletId,
  }) => apiCall(() async {
    final response = await _dio.post(
      '/api/orders/$orderId/cancel',
      data: CancelOrderRequest(walletId: walletId).toJson(),
    );
    return CancelOrderResponse.fromJson(response.data as Map<String, dynamic>);
  });
}

final orderRepositoryProvider = Provider<OrderRepository>(
  (ref) => OrderRepository(ref.watch(dioProvider)),
);
