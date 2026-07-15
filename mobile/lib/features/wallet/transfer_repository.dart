import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../models/cursor_page.dart';
import '../../models/enums.dart';
import '../../models/transfer.dart';

class TransferRepository {
  const TransferRepository(this._dio);

  final Dio _dio;

  /// 201 + `code: CREATED`. 멱등키가 중복이면 서버가 기존 `transferId` 를 `SUCCESS` 로 돌려준다.
  Future<TransferCoinResponse> transfer(TransferCoinRequest request) =>
      apiCall(() async {
        final response = await _dio.post(
          '/api/transfers',
          data: request.toJson(),
        );
        return TransferCoinResponse.fromJson(
          response.data as Map<String, dynamic>,
        );
      });

  /// 커서 이름이 **`cursorTransferId`(int)** 다. 웹은 `cursor`(문자열)를 보내서 페이지네이션이
  /// 실제로 동작하지 않았다(사양서 R4-8). 거래소 필터는 두지 않는다 — 내역은 이미 `walletId` 로
  /// 조회되며, 웹의 거래소 필터는 항상 공집합이 되는 결함이었다(R9).
  Future<CursorPage<TransferHistoryItem>> getTransferHistory({
    required int walletId,
    TransferType? type,
    int? cursorTransferId,
    int size = 20,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/wallets/$walletId/transfers',
      queryParameters: query({
        'type': type?.wire,
        'cursorTransferId': cursorTransferId,
        'size': size,
      }),
    );
    return CursorPage.fromJson(
      response.data as Map<String, dynamic>,
      (json) => TransferHistoryItem.fromJson(json as Map<String, dynamic>),
    );
  });
}

final transferRepositoryProvider = Provider<TransferRepository>(
  (ref) => TransferRepository(ref.watch(dioProvider)),
);
