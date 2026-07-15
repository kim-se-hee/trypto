import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/wallet.dart';

class WalletRepository {
  const WalletRepository(this._dio);

  final Dio _dio;

  /// 코인 잔고에는 심볼·현재가가 없다. 화면은 거래소 코인 목록과 `coinId` 로 합친다.
  Future<WalletBalances> getBalances(int walletId) => apiCall(() async {
    final response = await _dio.get('/api/wallets/$walletId/balances');
    return WalletBalances.fromJson(response.data as Map<String, dynamic>);
  });
}

final walletRepositoryProvider = Provider<WalletRepository>(
  (ref) => WalletRepository(ref.watch(dioProvider)),
);
